package org.upyog.family.domain.service;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry binding family OTP {@code referenceId} values to the mobile/tenant context
 * issued at send time, tracking failed validation attempts and one-time use.
 *
 * <p>Supports mock demo mode (fixed OTP still requires a valid, non-expired reference) and real
 * egov-otp flows where the reference UUID must match the send response.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
public class FamilyOtpSessionRegistry {

    /**
     * Mutable OTP challenge state keyed by reference identifier.
     */
    @Getter
    public static final class OtpChallenge {
        private final String referenceId;
        private final String mobileNumber;
        private final String tenantId;
        private final long expiresAtEpochMs;
        private final int maxAttempts;
        private int failedAttempts;
        private boolean used;

        /**
         * Creates a new OTP challenge record.
         *
         * @param referenceId       OTP reference returned to the client
         * @param mobileNumber      HoF mobile number the OTP was sent to
         * @param tenantId          tenant identifier
         * @param expiresAtEpochMs  epoch millis when the challenge expires
         * @param maxAttempts       maximum failed validation attempts allowed
         */
        public OtpChallenge(String referenceId, String mobileNumber, String tenantId,
                            long expiresAtEpochMs, int maxAttempts) {
            this.referenceId = referenceId;
            this.mobileNumber = mobileNumber;
            this.tenantId = tenantId;
            this.expiresAtEpochMs = expiresAtEpochMs;
            this.maxAttempts = maxAttempts;
        }

        /**
         * @return {@code true} if the challenge is past its expiry time
         */
        public boolean isExpired() {
            return Instant.now().toEpochMilli() > expiresAtEpochMs;
        }

        /**
         * @return {@code true} if failed attempts reached the configured maximum
         */
        public boolean isExhausted() {
            return failedAttempts >= maxAttempts;
        }

        /**
         * Records one failed OTP validation attempt.
         */
        public void recordFailedAttempt() {
            failedAttempts++;
        }

        /**
         * Marks the challenge as successfully consumed (OTP accepted).
         */
        public void markUsed() {
            this.used = true;
        }
    }

    private final Map<String, OtpChallenge> challengesByReferenceId = new ConcurrentHashMap<>();

    /**
     * Registers a newly issued OTP reference for later validation.
     *
     * @param referenceId  reference identifier from send OTP
     * @param mobileNumber recipient mobile number
     * @param tenantId     tenant identifier
     * @param ttlMs        time-to-live in milliseconds
     * @param maxAttempts  maximum failed validation attempts
     */
    public void register(String referenceId, String mobileNumber, String tenantId, long ttlMs, int maxAttempts) {
        if (StringUtils.isAnyBlank(referenceId, mobileNumber, tenantId)) {
            return;
        }
        long expiresAt = Instant.now().toEpochMilli() + Math.max(ttlMs, 1L);
        challengesByReferenceId.put(referenceId, new OtpChallenge(referenceId, mobileNumber, tenantId, expiresAt, maxAttempts));
    }

    /**
     * Resolves an active challenge for the given reference, mobile, and tenant.
     *
     * @param referenceId  client-supplied reference identifier
     * @param mobileNumber expected mobile number
     * @param tenantId     expected tenant identifier
     * @return matching {@link OtpChallenge}, or {@code null} if missing, expired, used, or context mismatch
     */
    public OtpChallenge resolveActiveChallenge(String referenceId, String mobileNumber, String tenantId) {
        if (StringUtils.isAnyBlank(referenceId, mobileNumber, tenantId)) {
            return null;
        }
        OtpChallenge challenge = challengesByReferenceId.get(referenceId);
        if (challenge == null) {
            return null;
        }
        if (challenge.isUsed() || challenge.isExpired() || challenge.isExhausted()) {
            challengesByReferenceId.remove(referenceId);
            return null;
        }
        if (!StringUtils.equals(challenge.getMobileNumber(), mobileNumber)
                || !StringUtils.equals(challenge.getTenantId(), tenantId)) {
            return null;
        }
        return challenge;
    }

    /**
     * Invalidates a challenge so it cannot be reused (e.g. after max failed attempts).
     *
     * @param referenceId reference identifier to remove
     */
    public void invalidate(String referenceId) {
        if (StringUtils.isNotBlank(referenceId)) {
            challengesByReferenceId.remove(referenceId);
        }
    }

    /**
     * Marks a challenge as successfully verified and removes it from the registry.
     *
     * @param challenge validated challenge
     */
    public void consume(OtpChallenge challenge) {
        if (challenge == null) {
            return;
        }
        challenge.markUsed();
        challengesByReferenceId.remove(challenge.getReferenceId());
    }
}
