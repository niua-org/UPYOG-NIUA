package org.upyog.family.domain.service;

import org.upyog.family.web.contract.FamilyOtpRequest;
import org.upyog.family.web.contract.FamilyOtpResponse;
import org.upyog.family.web.contract.FamilyOtpVerifyRequest;
import org.upyog.family.web.contract.FamilyOtpVerifyResponse;

/**
 * Domain service contract governing Head of Family (HoF) SMS OTP dispatch, verification, and consent issuance in UPYOG.
 *
 * <p>Enables the Family Join verification workflow: when an applicant citizen seeks to join an existing household,
 * this service dispatches an OTP to the current HoF's registered mobile number to obtain verifiable consent.
 * Once verified, a secure 128-bit consent token bound to the family and applicant is issued to permit direct entry into the household unit.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyOtpService {

    /**
     * Initiates SMS OTP dispatch to the Head of Family's registered mobile number for join verification.
     *
     * <p><b>Execution Workflow:</b></p>
     * <ol>
     *   <li>Retrieves the family unit matching {@code request.getFamilyId()} and {@code request.getTenantId()}.</li>
     *   <li>Queries {@code egov-user} to resolve the registered mobile number of the designated HoF.</li>
     *   <li>Masks the mobile number for display privacy (e.g. {@code ******3210}).</li>
     *   <li>Dispatches an OTP generation command to the platform SMS/OTP service (or fixed OTP in test profiles).</li>
     *   <li>Returns the masked mobile number and tracking {@code referenceId}.</li>
     * </ol>
     *
     * @param request the family OTP request containing target {@code familyId} and {@code tenantId}; must not be {@code null}.
     * @return {@link FamilyOtpResponse} containing masked mobile number, reference ID, and dispatch status.
     * @throws org.egov.tracer.model.CustomException if family is not found, HoF has no mobile registered, or OTP generation fails.
     */
    FamilyOtpResponse sendFamilyOtp(FamilyOtpRequest request);

    /**
     * Verifies the OTP entered by an applicant against the Head of Family's registered credentials.
     *
     * <p><b>Execution Workflow:</b></p>
     * <ol>
     *   <li>Locates the target household and resolves the HoF's registered mobile number.</li>
     *   <li>Validates the submitted OTP against {@code egov-otp} (or fixed development passwords if enabled).</li>
     *   <li>Upon successful validation, constructs a secure 128-bit consent token prefixed with {@code CONSENT-VERIFIED-}, bound to family and applicant.</li>
     *   <li>Enriches the family aggregate with member demographics for UI display.</li>
     *   <li>Returns the verified consent token alongside the complete family aggregate.</li>
     * </ol>
     *
     * @param request the family OTP verification request containing {@code familyId}, {@code tenantId}, and {@code otp};
     *                must not be {@code null}.
     * @return {@link FamilyOtpVerifyResponse} containing the enriched family details, consent token, and success status.
     * @throws org.egov.tracer.model.CustomException if the OTP is invalid or expired (Code: {@code INVALID_OTP}), or family is not found.
     */
    FamilyOtpVerifyResponse verifyFamilyOtp(FamilyOtpVerifyRequest request);
}
