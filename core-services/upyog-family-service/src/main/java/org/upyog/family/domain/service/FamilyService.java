package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.FamilySplitRequest;

import java.util.List;

/**
 * Domain service contract defining business operations for the UPYOG Family Registry.
 *
 * <p>Serves as the primary domain entry point managing citizen household units, membership lifecycles,
 * demographic enrichment, and structural evolutionary operations (split and merge). Enforces fundamental
 * business invariants across all lifecycle flows:</p>
 * <ul>
 *   <li><b>Single Active Head of Family (HoF):</b> Exactly one member within a family unit must have
 *       {@code isHead = true} and a corresponding {@code relationshipWithHead = "SELF"}.</li>
 *   <li><b>Multi-Family Exclusivity:</b> A citizen cannot be an active member of more than one household unit
 *       simultaneously across the platform.</li>
 *   <li><b>Dynamic Relationship Taxonomy:</b> Member relationships relative to the HoF are dynamically
 *       validated against state-level MDMS master data (e.g. {@code SPOUSE}, {@code SON}, {@code FATHER}).</li>
 *   <li><b>Event-Driven Asynchronous Persistence:</b> Modifications are dispatched via Kafka persister topics
 *       to guarantee eventual consistency, high write throughput, and full auditability.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyService {

    /**
     * Creates a new household Family Unit in UPYOG with sequential ID generation, audit enrichment, and persistence.
     *
     * <p><b>Execution Workflow:</b></p>
     * <ol>
     *   <li>Executes domain validation via {@code FamilyValidator#validateCreate} (checking tenant presence,
     *       unique member UUIDs, exactly one designated HoF, valid MDMS relationships, and absence of prior active memberships).</li>
     *   <li>Invokes UPYOG {@code idgen} to assign a state-wide monotonic family identifier (e.g. {@code FAM-2026-0001}).</li>
     *   <li>Enriches system audit details (created/last-modified timestamps and user UUID from {@link RequestInfo}).</li>
     *   <li>Dispatches the creation event to Kafka topic {@code persister.save.family.topic} for asynchronous DB ingestion.</li>
     *   <li>Synchronously queries {@code egov-user} to enrich member profiles with citizen demographic attributes (name, gender, mobile).</li>
     * </ol>
     *
     * @param request the family creation request envelope containing the {@link RequestInfo} and target {@link Family} entity;
     *                must not be {@code null}, and {@code request.getFamily().getMembers()} must contain at least one member.
     * @return the created {@link Family} aggregate populated with the generated {@code familyId}, active status, and enriched demographics.
     * @throws org.egov.tracer.model.CustomException if:
     *         <ul>
     *           <li>{@code request} or {@code request.getFamily()} is {@code null} (Code: {@code INVALID_REQUEST})</li>
     *           <li>{@code tenantId} is blank (Code: {@code INVALID_REQUEST})</li>
     *           <li>The roster does not contain exactly one active Head of Family (Code: {@code HEAD_OF_FAMILY_REQUIRED})</li>
     *           <li>Duplicate member UUIDs are detected in the request payload (Code: {@code INVALID_REQUEST})</li>
     *           <li>A member is already an active resident in another family (Code: {@code MEMBER_ALREADY_EXISTS})</li>
     *           <li>A member's relationship code is unrecognized by MDMS (Code: {@code INVALID_RELATIONSHIP})</li>
     *           <li>Referenced document IDs cannot be verified in FileStore (Code: {@code FILESTORE_NOT_FOUND})</li>
     *         </ul>
     */
    Family createFamily(FamilyRequest request);

    /**
     * Searches Family Units based on query filters such as family ID, member UUID, mobile number, or tenant.
     *
     * <p>Enforces multi-tenant scoping and enriches returned family aggregates with citizen profile information
     * (e.g. full name, mobile number, gender, date of birth) fetched in bulk from the citizen identity registry.</p>
     *
     * @param criteria query criteria containing search filters including {@code tenantId}, optional {@code familyId},
     *                 {@code memberUserUuid}, and {@code mobileNumber}; must not be {@code null}.
     * @param requestInfo UPYOG request context envelope containing caller identity and auth token; may be {@code null}.
     * @return a {@link List} of matching {@link Family} aggregates enriched with active member demographics,
     *         or an empty list if no households match the criteria.
     * @throws org.egov.tracer.model.CustomException if {@code criteria.getTenantId()} is blank (Code: {@code INVALID_REQUEST}).
     */
    List<Family> searchFamilies(FamilySearchCriteria criteria, RequestInfo requestInfo);

    /**
     * Updates an existing Family Unit's metadata and active member roster in UPYOG.
     *
     * <p>Supports modifying household category, adding or updating member relationships, managing soft-deactivations,
     * and attaching new verification documents. Validates that the resulting family retains exactly one active Head of Family.</p>
     *
     * @param request the update request envelope containing the modified {@link Family} object;
     *                must have valid {@code familyId} and {@code tenantId}.
     * @return the updated {@link Family} aggregate with updated audit details and enriched demographic attributes.
     * @throws org.egov.tracer.model.CustomException if:
     *         <ul>
     *           <li>The target family is not found or has an inactive/archived status (Code: {@code FAMILY_NOT_FOUND})</li>
     *           <li>The caller is unauthorized to mutate this household (Code: {@code UNAUTHORIZED_ACCESS})</li>
     *           <li>The update results in zero or more than one active Head of Family (Code: {@code HEAD_OF_FAMILY_REQUIRED})</li>
     *           <li>A newly added member already belongs to another active family (Code: {@code MEMBER_ALREADY_EXISTS})</li>
     *         </ul>
     */
    Family updateFamily(FamilyRequest request);

    /**
     * Splits an existing Family Unit into a child family in UPYOG, recording full genealogical lineage.
     *
     * <p><b>Business Workflow:</b></p>
     * <ol>
     *   <li>Verifies parent family exists, is currently {@code ACTIVE}, and is not currently locked.</li>
     *   <li>Validates that the departing members belong to the parent family and do not include the current HoF of the parent family.</li>
     *   <li>Soft-deactivates the departing members in the parent family unit and sets {@code exitReason = "FAMILY_SPLIT"}.</li>
     *   <li>Generates a new sequential {@code familyId} for the newly formed child unit.</li>
     *   <li>Assigns the designated {@code newHeadUserUuid} as the child unit's HoF.</li>
     *   <li>Persists a {@code FamilyLineage} audit record linking parent and child families with action type {@code SPLIT}.</li>
     *   <li>Emits Kafka update events for both parent and child families.</li>
     * </ol>
     *
     * @param request the family split request envelope specifying the {@code parentFamilyId}, list of splitting member UUIDs,
     *                the {@code newHeadUserUuid} for the child unit, and split rationale; must not be {@code null}.
     * @return the newly formed child {@link Family} aggregate populated with migrated active members and new HoF.
     * @throws org.egov.tracer.model.CustomException if:
     *         <ul>
     *           <li>Parent family is not found or inactive (Code: {@code FAMILY_NOT_FOUND})</li>
     *           <li>Caller is not the parent family's HoF or authorized administrator (Code: {@code UNAUTHORIZED_ACCESS})</li>
     *           <li>The proposed new head does not exist among the splitting members (Code: {@code HEAD_OF_FAMILY_REQUIRED})</li>
     *           <li>The split attempt would leave the parent family devoid of members (Code: {@code INVALID_REQUEST})</li>
     *         </ul>
     */
    Family splitFamily(FamilySplitRequest request);

    /**
     * Merges two existing Family Units into a consolidated household unit in UPYOG.
     *
     * <p><b>Business Workflow:</b></p>
     * <ol>
     *   <li>Validates that both the surviving and merging families exist, belong to the same tenant, and are {@code ACTIVE}.</li>
     *   <li>Transfers active members of the merging family into the surviving family roster.</li>
     *   <li>Updates member relationship codes to reflect their relationship with the consolidated {@code commonHeadUserUuid}.</li>
     *   <li>Transitions the merging family status to {@code MERGED} and archives it against future direct mutations.</li>
     *   <li>Inserts a {@code FamilyLineage} audit record with action type {@code MERGE}.</li>
     *   <li>Publishes asynchronous persistence messages to Kafka for database synchronization.</li>
     * </ol>
     *
     * @param request the merge request containing {@code survivingFamilyId}, {@code mergingFamilyId},
     *                {@code commonHeadUserUuid}, and legal/administrative merge reason; must not be {@code null}.
     * @return the consolidated surviving {@link Family} aggregate containing all combined members.
     * @throws org.egov.tracer.model.CustomException if:
     *         <ul>
     *           <li>Either family cannot be found or is already inactive/merged (Code: {@code FAMILY_NOT_FOUND})</li>
     *           <li>Families belong to disparate tenants (Code: {@code INVALID_REQUEST})</li>
     *           <li>The proposed common head is not an active member of either family (Code: {@code HEAD_OF_FAMILY_REQUIRED})</li>
     *           <li>Caller does not hold HoF authority over the participating families (Code: {@code UNAUTHORIZED_ACCESS})</li>
     *         </ul>
     */
    Family mergeFamily(FamilyMergeRequest request);
}
