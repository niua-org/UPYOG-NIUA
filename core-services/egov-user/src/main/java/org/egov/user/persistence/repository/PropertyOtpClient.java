package org.egov.user.persistence.repository;

import org.egov.common.contract.request.RequestInfo;

/**
 * Repository interface governing communication with external OTP providers for property linking workflows.
 *
 * <p>Adheres to the <b>Dependency Inversion Principle (DIP)</b> and <b>Open/Closed Principle (OCP)</b>
 * by decoupling higher-level domain orchestration from concrete communication channels (REST / Kafka / fixed OTP).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface PropertyOtpClient {

    /**
     * Dispatches an OTP challenge to the designated property owner mobile number.
     *
     * @param mobileNumber destination mobile number
     * @param tenantId tenant identifier
     * @param requestInfo platform request context
     */
    void sendOtp(String mobileNumber, String tenantId, RequestInfo requestInfo);

    /**
     * Validates an entered OTP against the underlying OTP authority or configured fixed values.
     *
     * @param mobileNumber identity mobile number
     * @param otp entered OTP digits
     * @param tenantId tenant identifier
     * @param requestInfo platform request context
     * @return true if OTP is valid, false otherwise
     */
    boolean validateOtp(String mobileNumber, String otp, String tenantId, RequestInfo requestInfo);
}
