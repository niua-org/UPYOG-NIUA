package org.upyog.family.persistence.repository;

import org.egov.common.contract.request.RequestInfo;

/**
 * Client interface communicating with UPYOG OTP services for creating, dispatching, and validating citizen OTPs.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface OtpClient {

    /**
     * Creates and dispatches an OTP to the given mobile number for a specific tenant.
     *
     * @param mobileNumber the recipient citizen mobile number
     * @param tenantId     the tenant identifier
     * @param requestInfo  UPYOG request context envelope
     * @return reference identifier / token UUID generated for the OTP transaction
     */
    String sendOtp(String mobileNumber, String tenantId, RequestInfo requestInfo);

    /**
     * Validates the submitted OTP against the recipient's mobile number and tenant.
     *
     * @param mobileNumber the recipient citizen mobile number
     * @param otp          the OTP code submitted by the applicant
     * @param tenantId     the tenant identifier
     * @param requestInfo  UPYOG request context envelope
     * @return true if the OTP is valid and matches; false otherwise
     */
    boolean validateOtp(String mobileNumber, String otp, String tenantId, RequestInfo requestInfo);
}
