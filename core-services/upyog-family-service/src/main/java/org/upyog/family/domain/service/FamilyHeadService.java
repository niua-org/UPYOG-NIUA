package org.upyog.family.domain.service;

import org.upyog.family.web.contract.AssignHofRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyHofCheckRequest;
import org.upyog.family.web.contract.FamilyHofCheckResponse;

/**
 * Domain service contract governing Head of Family (HoF) succession and leadership transfer flows in UPYOG.
 *
 * <p>Enforces statutory business rules governing household representation:</p>
 * <ul>
 *   <li>Only the existing Head of Family or an authorized administrator can transfer the HoF role.</li>
 *   <li>The prospective new Head must already be an active, verified member of the household unit.</li>
 *   <li>The member cannot already be the current Head of Family.</li>
 *   <li>Upon succession, the existing HoF's leadership flag is revoked, and the new Head is designated with {@code relationshipWithHead = "HEAD"}.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyHeadService {

    /**
     * Transfers or assigns the Head of Family role to an existing active member of the family unit in UPYOG.
     *
     * <p><b>Execution Workflow:</b></p>
     * <ol>
     *   <li>Retrieves the target household unit and verifies its status is {@code ACTIVE}.</li>
     *   <li>Validates caller authorization ensuring only the current HoF initiates succession.</li>
     *   <li>Verifies that {@code newHeadUserUuid} is an active, verified resident of the family.</li>
     *   <li>Revokes {@code isHead} from the current HoF and sets {@code isHead = true} on the new HoF.</li>
     *   <li>Updates {@code family.headUserUuid} and dispatches update commands to Kafka persister.</li>
     * </ol>
     *
     * @param request the HoF assignment payload containing {@code familyId}, {@code newHeadUserUuid},
     *                and succession reason/justification; must not be {@code null}.
     * @return the updated {@link Family} aggregate reflecting the succession change.
     * @throws org.egov.tracer.model.CustomException if:
     *         <ul>
     *           <li>Target family is not found or inactive (Code: {@code FAMILY_NOT_FOUND})</li>
     *           <li>Caller is not the current HoF (Code: {@code UNAUTHORIZED_ACCESS})</li>
     *           <li>Proposed head is already the active HoF (Code: {@code ALREADY_HEAD})</li>
     *           <li>Proposed head is not an active member in the family (Code: {@code MEMBER_NOT_FOUND} or {@code MEMBER_INACTIVE})</li>
     *         </ul>
     */
    Family assignHeadOfFamily(AssignHofRequest request);

    /**
     * Checks whether the authenticated caller in {@link RequestInfo} is the designated Head of Family (HoF)
     * for the specified family.
     *
     * @param request the HoF check request containing RequestInfo, familyId, and optional tenantId; must not be {@code null}.
     * @return {@link FamilyHofCheckResponse} containing {@code isHof} flag, caller {@code userUuid}, and {@code headUserUuid}.
     * @throws org.egov.tracer.model.CustomException if RequestInfo or user UUID is missing, or family is not found.
     */
    FamilyHofCheckResponse isHeadOfFamily(FamilyHofCheckRequest request);
}
