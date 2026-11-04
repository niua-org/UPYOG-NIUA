package org.upyog.family.persistence.repository;

import org.apache.commons.lang3.StringUtils;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyLineage;
import org.upyog.family.web.contract.FamilySearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction interface for Family unit aggregates and genealogical lineage history in UPYOG.
 *
 * <p>Encapsulates all PostgreSQL JDBC operations for {@code ug_family}, {@code ug_family_member},
 * {@code ug_family_document}, and {@code ug_family_lineage} tables. Supports multi-tenant query resolution,
 * single-family lookups, citizen residency checks, and genealogical lineage auditing.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyRepository {

    /**
     * Persists a new Family aggregate along with its members and document proofs to PostgreSQL.
     *
     * <p>Inserts the parent record into {@code ug_family}, batch inserts associated members into
     * {@code ug_family_member}, and writes attached verification documents into {@code ug_family_document}.</p>
     *
     * @param family the {@link Family} aggregate to persist; must have valid {@code familyId} and {@code tenantId}.
     * @return the persisted {@link Family} aggregate with database IDs and audit details intact.
     */
    Family save(Family family);

    /**
     * Updates an existing Family aggregate, active member records, and document proofs in PostgreSQL.
     *
     * <p>Modifies core attributes in {@code ug_family}, updates or inserts members in {@code ug_family_member},
     * and synchronizes document rows in {@code ug_family_document}.</p>
     *
     * @param family the updated {@link Family} aggregate containing modified fields and member statuses.
     * @return the persisted updated {@link Family} aggregate.
     */
    Family update(Family family);

    /**
     * Searches Family aggregates based on filter criteria (family ID, member UUID, or tenant ID).
     *
     * <p>Executes a multi-join query via {@link org.upyog.family.persistence.builder.FamilyQueryBuilder}
     * aggregating household headers, child members, and document references.</p>
     *
     * @param criteria search criteria filter object; must specify valid {@code tenantId}.
     * @return a {@link List} of matching {@link Family} aggregates, or an empty list if none match.
     */
    List<Family> search(FamilySearchCriteria criteria);

    /**
     * Looks up an individual Family aggregate by its unique {@code familyId} and {@code tenantId}.
     *
     * <p>Provides a clean {@link Optional} abstraction replacing legacy {@code search(criteria).get(0)}
     * anti-patterns across domain services.</p>
     *
     * @param familyId unique family identifier; if blank, returns {@link Optional#empty()}.
     * @param tenantId tenant identifier; if blank, returns {@link Optional#empty()}.
     * @return an {@link Optional} containing the {@link Family} aggregate if found, or empty otherwise.
     */
    default Optional<Family> findByFamilyIdAndTenantId(String familyId, String tenantId) {
        if (StringUtils.isBlank(familyId) || StringUtils.isBlank(tenantId)) {
            return Optional.empty();
        }
        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .familyId(familyId)
                .tenantId(tenantId)
                .limit(1)
                .offset(0)
                .build();
        List<Family> families = search(criteria);
        return (families != null && !families.isEmpty()) ? Optional.of(families.get(0)) : Optional.empty();
    }

    /**
     * Finds active Family IDs where the specified citizen UUID is registered as an active resident member.
     *
     * <p>Used to enforce platform-wide multi-family residency exclusivity before permitting citizen addition,
     * joining, or new household registration.</p>
     *
     * @param userUuid unique citizen identifier; must not be blank.
     * @return a {@link List} of active family ID strings currently occupied by the citizen; empty if none.
     */
    List<String> findActiveFamilyIdsByMemberUuid(String userUuid);

    /**
     * Records a family lifecycle genealogical lineage event (SPLIT or MERGE) into {@code ug_family_lineage}.
     *
     * @param lineage the lineage record object specifying parent/child family IDs, action type, and reason.
     */
    void saveLineage(FamilyLineage lineage);

    /**
     * Retrieves all genealogical lineage events associated with a family identifier.
     *
     * @param familyId the unique family identifier; must not be blank.
     * @return a {@link List} of {@link FamilyLineage} event records sorted by creation timestamp.
     */
    List<FamilyLineage> findLineageByFamilyId(String familyId);
}
