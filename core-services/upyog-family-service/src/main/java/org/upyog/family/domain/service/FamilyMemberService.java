package org.upyog.family.domain.service;

import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.JoinFamilyRequest;
import org.upyog.family.web.contract.LeaveFamilyRequest;

/**
 * Domain service contract governing individual family member lifecycle operations (Add, Join, Leave) in UPYOG.
 *
 * <p>Enforces citizen consent protocols, Aadhaar e-KYC verification, multi-family residency exclusivity,
 * and exit audit trails across all member-level mutations.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyMemberService {

    /**
     * Adds a new verified member to an existing family unit with verified Aadhaar consent.
     *
     * <p><b>Execution Workflow:</b></p>
     * <ol>
     *   <li>Verifies caller is the designated Head of Family (HoF) for the target household.</li>
     *   <li>Validates the candidate's relationship against state MDMS master data.</li>
     *   <li>Verifies that the candidate is not already an active resident of any other family.</li>
     *   <li>Validates the Aadhaar OTP consent token with {@code requester-services-dx}.</li>
     *   <li>Appends the member to the family aggregate and dispatches an update event to Kafka.</li>
     * </ol>
     *
     * @param request the member addition payload containing target {@code familyId}, member demographics/UUID,
     *                relationship with HoF, and Aadhaar consent token; must not be {@code null}.
     * @return the updated {@link Family} aggregate populated with the newly added active member.
     * @throws org.egov.tracer.model.CustomException if:
     *         <ul>
     *           <li>Target family is missing or inactive (Code: {@code FAMILY_NOT_FOUND})</li>
     *           <li>Caller is not the authorized HoF (Code: {@code UNAUTHORIZED_ACCESS})</li>
     *           <li>Candidate citizen is already in an active family (Code: {@code MEMBER_ALREADY_EXISTS})</li>
     *           <li>Aadhaar consent verification fails (Code: {@code INVALID_CONSENT})</li>
     *           <li>Relationship code is not found in MDMS (Code: {@code INVALID_RELATIONSHIP})</li>
     *         </ul>
     */
    Family addMember(AddMemberRequest request);

    /**
     * Processes an authenticated citizen's request to directly join an existing family upon verified HoF consent.
     *
     * <p><b>Execution Workflow:</b></p>
     * <ol>
     *   <li>Verifies the applicant citizen is authenticated and possesses a valid {@code applicantUserUuid}.</li>
     *   <li>Validates the HoF OTP consent token issued via {@code /family/v1/family/_verifyOtp} (prefixed with {@code CONSENT-VERIFIED-}).</li>
     *   <li>Enforces multi-family exclusivity ensuring the applicant is not an active member in another household.</li>
     *   <li>Directly appends the member to the family roster with status {@code JOINED} (active = true).</li>
     *   <li>Dispatches the updated family aggregate to the Kafka persister topic.</li>
     * </ol>
     *
     * @param request the join family request containing {@code familyId}, {@code applicantUserUuid}, relationship,
     *                and the verified HoF consent token; must not be {@code null}.
     * @return the updated {@link Family} aggregate with the applicant included as an active member.
     * @throws org.egov.tracer.model.CustomException if:
     *         <ul>
     *           <li>Target family does not exist (Code: {@code FAMILY_NOT_FOUND})</li>
     *           <li>Consent token is missing, expired, or invalid (Code: {@code INVALID_CONSENT})</li>
     *           <li>Applicant is already an active member of another family (Code: {@code MEMBER_ALREADY_EXISTS})</li>
     *         </ul>
     */
    Family joinFamily(JoinFamilyRequest request);

    /**
     * Processes a member's exit from a family unit due to marriage, demise, migration, or personal independence.
     *
     * <p><b>Execution Workflow:</b></p>
     * <ol>
     *   <li>Validates caller authorization: caller must be either the leaving member themselves or the family HoF.</li>
     *   <li>Ensures the departing member is not the sole active HoF (HoF must transfer leadership before exiting).</li>
     *   <li>Soft-deactivates the member by setting {@code active = false}, {@code status = INACTIVE},
     *       {@code exitReason}, and {@code exitDate = now}.</li>
     *   <li>Pushes the modification to the Kafka persister topic and returns the updated family.</li>
     * </ol>
     *
     * @param request the leave request specifying {@code familyId}, departing {@code memberUserUuid},
     *                valid exit reason (e.g. {@code DEATH}, {@code MARRIAGE}, {@code RELOCATION}), and optional remarks.
     * @return the updated {@link Family} aggregate reflecting the deactivated member.
     * @throws org.egov.tracer.model.CustomException if:
     *         <ul>
     *           <li>Target family or member is not found (Code: {@code FAMILY_NOT_FOUND} or {@code MEMBER_NOT_FOUND})</li>
     *           <li>Departing member is the active HoF (Code: {@code INVALID_REQUEST} - HoF succession required first)</li>
     *           <li>Caller is unauthorized to remove this member (Code: {@code UNAUTHORIZED_ACCESS})</li>
     *         </ul>
     */
    Family leaveFamily(LeaveFamilyRequest request);
}
