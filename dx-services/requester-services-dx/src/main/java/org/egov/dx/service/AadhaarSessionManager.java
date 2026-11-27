package org.egov.dx.service;

/**
 * Interface contract for managing Aadhaar OTP verification sessions, rate limiting,
 * and replay-prevention tracking.
 *
 * <p>Adheres strictly to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Encapsulates session attempt tracking and replay protection.</li>
 *   <li><b>Interface Segregation Principle (ISP):</b> Exposes only cohesive session management methods.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Backing stores (in-memory, Redis, distributed cache) can be swapped without changing callers.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface AadhaarSessionManager {

    /**
     * Records a failed OTP verification attempt for the given reference ID.
     *
     * @param refId session reference ID
     */
    void recordFailedOtpAttempt(String refId);

    /**
     * Retrieves the count of failed OTP attempts for the given reference ID.
     *
     * @param refId session reference ID
     * @return current number of failed attempts
     */
    int getFailedOtpAttempts(String refId);

    /**
     * Clears failed OTP attempt tracking upon successful verification.
     *
     * @param refId session reference ID
     */
    void clearFailedOtpAttempts(String refId);

    /**
     * Checks if the reference ID has already been verified and consumed.
     *
     * @param refId session reference ID
     * @return true if consumed, false otherwise
     */
    boolean isReferenceIdConsumed(String refId);

    /**
     * Marks the reference ID as consumed to prevent replay attacks.
     *
     * @param refId session reference ID
     */
    void markReferenceIdConsumed(String refId);

    /**
     * Checks if the failed OTP attempts have exceeded the configured maximum limit.
     *
     * @param refId session reference ID
     * @return true if limit exceeded, false otherwise
     */
    boolean isAttemptLimitExceeded(String refId);

    /**
     * Gets the configured maximum OTP attempts.
     *
     * @return max attempts threshold
     */
    int getMaxOtpAttempts();

    /**
     * Associates the citizen's Aadhaar number with the active session reference ID.
     *
     * @param refId session reference ID
     * @param aadhaarNumber raw 12-digit or masked Aadhaar identifier
     */
    void storeAadhaarForReferenceId(String refId, String aadhaarNumber);

    /**
     * Retrieves the Aadhaar number associated with the active session reference ID.
     *
     * @param refId session reference ID
     * @return Aadhaar number or null if not found/expired
     */
    String getAadhaarForReferenceId(String refId);

    /**
     * Associates the citizen's mobile number with the active session reference ID.
     *
     * @param refId session reference ID
     * @param mobileNumber 10-digit mobile number
     */
    void storeMobileForReferenceId(String refId, String mobileNumber);

    /**
     * Retrieves the mobile number associated with the active session reference ID.
     *
     * @param refId session reference ID
     * @return mobile number or null if not found/expired
     */
    String getMobileForReferenceId(String refId);
}
