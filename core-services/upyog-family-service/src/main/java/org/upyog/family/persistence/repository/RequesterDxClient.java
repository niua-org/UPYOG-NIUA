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
     * Validates an Aadhaar OTP verification consent token or OTP.
     *
     * @param consentToken the verification token or OTP string
     * @param tenantId     the tenant identifier
     * @param requestInfo  UPYOG request context
     * @return true if consent is verified, false otherwise
     */
    boolean verifyConsentToken(String consentToken, String tenantId, RequestInfo requestInfo);
}
