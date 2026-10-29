package org.upyog.family.persistence.repository;

import org.egov.common.contract.request.RequestInfo;

/**
 * Abstraction interface for communicating with requester-services-dx Aadhaar gateway and consent verification in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface RequesterDxClient {

    /**
     * Validates an Aadhaar OTP verification consent token or OTP tied to a specific citizen and family.
     *
     * @param consentToken        the verification token (or referenceId:otp)
     * @param memberUuidOrAadhaar citizen UUID or Aadhaar reference of the member being added
     * @param familyId            the target family identifier
     * @param tenantId            the tenant identifier
     * @param requestInfo         UPYOG request context
     * @return true if consent is verified, false otherwise
     */
    boolean verifyConsentToken(String consentToken, String memberUuidOrAadhaar, String familyId, String tenantId, RequestInfo requestInfo);

    /**
     * Backward-compatible overload for validating consent tokens.
     *
     * @param consentToken the verification token or OTP string
     * @param tenantId     the tenant identifier
     * @param requestInfo  UPYOG request context
     * @return true if consent is verified, false otherwise
     */
    default boolean verifyConsentToken(String consentToken, String tenantId, RequestInfo requestInfo) {
        return verifyConsentToken(consentToken, null, null, tenantId, requestInfo);
    }
}
