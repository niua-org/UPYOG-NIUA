package org.egov.user.domain.service.identity;

import org.egov.common.contract.request.RequestInfo;

/**
 * Service contract for verifying identity documents (e.g. Aadhaar e-KYC) with external identity providers.
 *
 * Adheres to Dependency Inversion Principle (DIP).
 */
public interface IdentityVerificationService {

    /**
     * Verifies Aadhaar OTP and retrieves verified demographic attributes.
     *
     * @param requestInfo caller RequestInfo
     * @param tenantId state or ULB tenant
     * @param refId transaction or reference ID
     * @param rawAadhaar plain or masked Aadhaar number
     * @param otp entered OTP
     * @param isResync true if called during re-sync flow
     * @return verified demographic attributes
     */
    VerifiedAadhaarDemographics verifyAadhaarOtp(RequestInfo requestInfo,
                                                String tenantId,
                                                String refId,
                                                String rawAadhaar,
                                                String otp,
                                                boolean isResync);
}
