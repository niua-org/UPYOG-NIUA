package org.egov.user.domain.service.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.util.MaskUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe session registry for managing Urban Property OTP challenges, rate-limiting,
 * and verified property consent tokens with optional Redis cluster persistence and in-memory fallback.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class UrbanPropertyOtpSessionRegistry {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, OtpChallenge> activeChallenges = new ConcurrentHashMap<>();
    private final Map<String, VerifiedPropertyConsent> verifiedConsents = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> localOtpSendTimestamps = new ConcurrentHashMap<>();

    /**
     * Primary constructor injecting optional {@link RedisTemplate} for distributed session storage.
     *
     * @param redisTemplate Spring RedisTemplate for distributed session management (optional)
     */
    @Autowired
    public UrbanPropertyOtpSessionRegistry(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Default constructor for testing without Redis support.
     */
    public UrbanPropertyOtpSessionRegistry() {
        this(null);
    }

    /**
     * Represents an active OTP challenge issued for property linking verification.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class OtpChallenge implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String referenceId;
        private final String propertyId;
        private final String tenantId;
        private final String mobileNumber;
        private final String callerUuid;
        private final long expiresAt;
        private final int maxAttempts;
        @Builder.Default
        private final AtomicInteger failedAttempts = new AtomicInteger(0);

        /**
         * Checks whether the current system time has surpassed the challenge expiry timestamp.
         *
         * @return {@code true} if current epoch time exceeds {@code expiresAt}, {@code false} otherwise.
         */
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        /**
         * Atomically increments and records a failed OTP verification attempt against this challenge.
         *
         * @return the newly incremented total count of failed attempts.
         */
        public int recordFailedAttempt() {
            return failedAttempts.incrementAndGet();
        }

        /**
         * Checks whether the count of recorded failed verification attempts has reached or exceeded maxAttempts.
         *
         * @return {@code true} if failed attempts &gt;= maxAttempts, {@code false} otherwise.
         */
        public boolean isExhausted() {
            return failedAttempts.get() >= maxAttempts;
        }
    }

    /**
     * Represents an authenticated consent token issued upon successful OTP verification.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class VerifiedPropertyConsent implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String consentToken;
        private final String propertyId;
        private final String tenantId;
        private final String mobileNumber;
        private final String ownerUuid;
        private final String callerUuid;
        private final long verifiedTime;
        private final long expiresAt;
        @Builder.Default
        private boolean consumed = false;

        /**
         * Checks whether this verified consent token has expired based on wall-clock epoch time.
         *
         * @return {@code true} if current epoch time exceeds {@code expiresAt}, {@code false} otherwise.
         */
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        /**
         * Indicates whether this single-use consent token has already been consumed for property linking.
         *
         * @return {@code true} if consumed, {@code false} if still active and available.
         */
        public boolean isConsumed() {
            return consumed;
        }

        /**
         * Marks this consent token as consumed so that it cannot be re-used in any subsequent linking request.
         */
        public void markConsumed() {
            this.consumed = true;
        }
    }

    /**
     * Checks whether an OTP dispatch request is allowed under rate-limiting policies
     * (maximum 3 dispatches per 15-minute sliding window per property to prevent SMS flooding).
     *
     * @param propertyId municipal property identifier
     * @return {@code true} if dispatch is permitted, {@code false} if rate-limited
     */
    public boolean isOtpDispatchAllowed(String propertyId) {
        if (StringUtils.isBlank(propertyId)) {
            return false;
        }
        String rateKey = propertyId.trim().toUpperCase();
        long now = System.currentTimeMillis();
        long windowStart = now - UserConstants.PROPERTY_OTP_SEND_WINDOW_MS;

        List<Long> timestamps = localOtpSendTimestamps.get(rateKey);
        if (timestamps == null) {
            return true;
        }
        synchronized (timestamps) {
            timestamps.removeIf(ts -> ts < windowStart);
            return timestamps.size() < UserConstants.MAX_PROPERTY_OTP_SENDS_PER_WINDOW;
        }
    }

    /**
     * Records an OTP dispatch event in the rate-limiting tracking registry.
     *
     * @param propertyId municipal property identifier
     */
    public void recordOtpDispatch(String propertyId) {
        if (StringUtils.isBlank(propertyId)) {
            return;
        }
        String rateKey = propertyId.trim().toUpperCase();
        long now = System.currentTimeMillis();
        localOtpSendTimestamps.compute(rateKey, (k, v) -> {
            List<Long> list = v != null ? v : Collections.synchronizedList(new ArrayList<>());
            list.add(now);
            return list;
        });
    }

    /**
     * Registers a new active OTP challenge for property ownership verification.
     *
     * @param referenceId tracking reference identifier
     * @param propertyId municipal property identifier
     * @param tenantId tenant identifier
     * @param mobileNumber destination mobile number receiving the OTP
     * @param callerUuid optional caller citizen UUID initiating the link
     * @param ttlMs session duration in milliseconds
     * @param maxAttempts maximum allowed failed verification attempts
     */
    public void registerChallenge(String referenceId, String propertyId, String tenantId,
                                  String mobileNumber, String callerUuid, long ttlMs, int maxAttempts) {
        cleanExpiredChallenges();
        long now = System.currentTimeMillis();
        OtpChallenge challenge = OtpChallenge.builder()
                .referenceId(referenceId)
                .propertyId(propertyId.trim())
                .tenantId(tenantId.trim())
                .mobileNumber(mobileNumber.trim())
                .callerUuid(callerUuid != null ? callerUuid.trim() : null)
                .expiresAt(now + ttlMs)
                .maxAttempts(maxAttempts)
                .build();

        activeChallenges.put(referenceId, challenge);

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(UserConstants.REDIS_PROP_OTP_CHALLENGE_PREFIX + referenceId, challenge, ttlMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis write failed for challenge {}: {}", MaskUtils.maskIdentifier(referenceId), e.getMessage());
            }
        }

        log.info("[PROPERTY-OTP-REGISTRY] Registered challenge: {} for property: {}, mobile: {}",
                MaskUtils.maskIdentifier(referenceId), propertyId, MaskUtils.maskMobile(mobileNumber));
    }

    /**
     * Backward-compatible registration without callerUuid.
     */
    public void registerChallenge(String referenceId, String propertyId, String tenantId,
                                  String mobileNumber, long ttlMs, int maxAttempts) {
        registerChallenge(referenceId, propertyId, tenantId, mobileNumber, null, ttlMs, maxAttempts);
    }

    /**
     * Resolves an unexpired, unexhausted challenge matching reference ID, property ID, and tenant.
     *
     * @param referenceId tracking reference identifier
     * @param propertyId municipal property identifier
     * @param tenantId tenant identifier
     * @return active OtpChallenge if valid, or null
     */
    public OtpChallenge resolveActiveChallenge(String referenceId, String propertyId, String tenantId) {
        cleanExpiredChallenges();
        if (StringUtils.isBlank(referenceId)) {
            return null;
        }

        OtpChallenge challenge = null;
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(UserConstants.REDIS_PROP_OTP_CHALLENGE_PREFIX + referenceId.trim());
                if (cached instanceof OtpChallenge) {
                    challenge = (OtpChallenge) cached;
                }
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis read failed for challenge {}: {}", MaskUtils.maskIdentifier(referenceId), e.getMessage());
            }
        }

        if (challenge == null) {
            challenge = activeChallenges.get(referenceId.trim());
        }

        if (challenge == null) {
            return null;
        }
        if (challenge.isExpired() || challenge.isExhausted()) {
            invalidateChallenge(referenceId);
            return null;
        }
        if (!challenge.getPropertyId().equalsIgnoreCase(propertyId.trim())
                || !challenge.getTenantId().equalsIgnoreCase(tenantId.trim())) {
            log.warn("[PROPERTY-OTP-REGISTRY] Mismatched challenge details for reference: {}", MaskUtils.maskIdentifier(referenceId));
            return null;
        }
        return challenge;
    }

    /**
     * Invalidates and removes an active OTP challenge from the registry.
     *
     * @param referenceId tracking reference identifier to remove
     */
    public void invalidateChallenge(String referenceId) {
        if (StringUtils.isNotBlank(referenceId)) {
            String key = referenceId.trim();
            activeChallenges.remove(key);
            if (redisTemplate != null) {
                try {
                    redisTemplate.delete(UserConstants.REDIS_PROP_OTP_CHALLENGE_PREFIX + key);
                } catch (Exception e) {
                    log.warn("[PROPERTY-OTP-REGISTRY] Redis delete failed for challenge {}: {}", MaskUtils.maskIdentifier(key), e.getMessage());
                }
            }
        }
    }

    /**
     * Registers a verified property consent token upon successful OTP verification.
     *
     * @param consentToken issued unique consent token string
     * @param propertyId municipal property identifier
     * @param tenantId tenant identifier
     * @param mobileNumber verified property owner mobile number
     * @param ownerUuid optional verified citizen owner UUID
     * @param callerUuid optional caller citizen UUID bound to this token
     * @param ttlMs token validity duration in milliseconds
     */
    public void registerConsentToken(String consentToken, String propertyId, String tenantId,
                                     String mobileNumber, String ownerUuid, String callerUuid, long ttlMs) {
        cleanExpiredConsents();
        long now = System.currentTimeMillis();
        VerifiedPropertyConsent consent = VerifiedPropertyConsent.builder()
                .consentToken(consentToken)
                .propertyId(propertyId.trim())
                .tenantId(tenantId.trim())
                .mobileNumber(mobileNumber != null ? mobileNumber.trim() : null)
                .ownerUuid(ownerUuid != null ? ownerUuid.trim() : null)
                .callerUuid(callerUuid != null ? callerUuid.trim() : null)
                .verifiedTime(now)
                .expiresAt(now + ttlMs)
                .consumed(false)
                .build();

        verifiedConsents.put(consentToken, consent);

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(UserConstants.REDIS_PROP_CONSENT_PREFIX + consentToken, consent, ttlMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis write failed for consent {}: {}", MaskUtils.maskIdentifier(consentToken), e.getMessage());
            }
        }

        log.info("[PROPERTY-OTP-REGISTRY] Registered verified property consent token: {} for property: {}, caller: {}",
                MaskUtils.maskIdentifier(consentToken), propertyId, callerUuid);
    }

    /**
     * Backward-compatible registration without callerUuid.
     */
    public void registerConsentToken(String consentToken, String propertyId, String tenantId,
                                     String mobileNumber, String ownerUuid, long ttlMs) {
        registerConsentToken(consentToken, propertyId, tenantId, mobileNumber, ownerUuid, null, ttlMs);
    }

    /**
     * Resolves a consent token from Redis or local map.
     */
    private VerifiedPropertyConsent resolveConsent(String consentToken) {
        if (StringUtils.isBlank(consentToken)) {
            return null;
        }
        String key = consentToken.trim();
        VerifiedPropertyConsent consent = null;
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(UserConstants.REDIS_PROP_CONSENT_PREFIX + key);
                if (cached instanceof VerifiedPropertyConsent) {
                    consent = (VerifiedPropertyConsent) cached;
                }
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis read failed for consent {}: {}", MaskUtils.maskIdentifier(key), e.getMessage());
            }
        }
        if (consent == null) {
            consent = verifiedConsents.get(key);
        }
        return consent;
    }

    /**
     * Removes a consent token from Redis and local map.
     */
    private void removeConsent(String consentToken) {
        if (StringUtils.isNotBlank(consentToken)) {
            String key = consentToken.trim();
            verifiedConsents.remove(key);
            if (redisTemplate != null) {
                try {
                    redisTemplate.delete(UserConstants.REDIS_PROP_CONSENT_PREFIX + key);
                } catch (Exception e) {
                    log.warn("[PROPERTY-OTP-REGISTRY] Redis delete failed for consent {}: {}", MaskUtils.maskIdentifier(key), e.getMessage());
                }
            }
        }
    }

    /**
     * Checks if there is any unexpired, unconsumed verified consent for the given propertyId.
     *
     * @param propertyId municipal property identifier
     * @return true if an unexpired verified consent exists for this property, false otherwise
     */
    public boolean hasActiveVerifiedConsentForProperty(String propertyId) {
        cleanExpiredConsents();
        if (StringUtils.isBlank(propertyId)) {
            return false;
        }
        return verifiedConsents.values().stream()
                .anyMatch(c -> c.getPropertyId().equalsIgnoreCase(propertyId.trim()) && !c.isExpired() && !c.isConsumed());
    }

    /**
     * Validates whether a consent token exists, is unexpired, and matches propertyId, callerUuid, and tenantId.
     *
     * @param propertyId municipal property identifier
     * @param consentToken issued consent token string
     * @param callerUuid citizen caller UUID attempting to link the property
     * @param tenantId tenant identifier
     * @return true if valid and active, false otherwise
     */
    public boolean validateConsentToken(String propertyId, String consentToken, String callerUuid, String tenantId) {
        cleanExpiredConsents();
        if (StringUtils.isBlank(propertyId) || StringUtils.isBlank(consentToken)) {
            return false;
        }
        VerifiedPropertyConsent consent = resolveConsent(consentToken.trim());
        if (consent == null) {
            return false;
        }
        if (consent.isExpired() || consent.isConsumed()) {
            removeConsent(consentToken.trim());
            return false;
        }
        if (!consent.getPropertyId().equalsIgnoreCase(propertyId.trim())) {
            return false;
        }
        if (StringUtils.isNotBlank(tenantId) && StringUtils.isNotBlank(consent.getTenantId())
                && !consent.getTenantId().equalsIgnoreCase(tenantId.trim())) {
            return false;
        }
        if (StringUtils.isNotBlank(callerUuid) && StringUtils.isNotBlank(consent.getCallerUuid())
                && !consent.getCallerUuid().equals(callerUuid.trim())) {
            log.warn("[PROPERTY-OTP-REGISTRY] Caller UUID mismatch for consent {}: expected {}, got {}",
                    MaskUtils.maskIdentifier(consentToken), consent.getCallerUuid(), callerUuid);
            return false;
        }
        return true;
    }

    /**
     * Validates whether a consent token exists, is unexpired, and belongs to the given propertyId.
     *
     * @param propertyId municipal property identifier
     * @param consentToken issued consent token string
     * @return true if valid and active, false otherwise
     */
    public boolean validateConsentToken(String propertyId, String consentToken) {
        return validateConsentToken(propertyId, consentToken, null, null);
    }

    /**
     * Validates and consumes (removes) a single-use consent token, ensuring it belongs to the
     * target propertyId, callerUuid, and tenantId.
     *
     * @param propertyId municipal property identifier
     * @param consentToken issued consent token string
     * @param callerUuid citizen caller UUID attempting to link the property
     * @param tenantId tenant identifier
     * @return true if successfully consumed, false otherwise
     */
    public boolean consumeConsentToken(String propertyId, String consentToken, String callerUuid, String tenantId) {
        if (validateConsentToken(propertyId, consentToken, callerUuid, tenantId)) {
            VerifiedPropertyConsent consent = resolveConsent(consentToken.trim());
            if (consent != null) {
                consent.markConsumed();
            }
            removeConsent(consentToken.trim());
            return true;
        }
        return false;
    }

    /**
     * Backward-compatible consume method without caller/tenant checks.
     */
    public boolean consumeConsentToken(String propertyId, String consentToken) {
        return consumeConsentToken(propertyId, consentToken, null, null);
    }

    private void cleanExpiredChallenges() {
        activeChallenges.entrySet().removeIf(e -> e.getValue().isExpired() || e.getValue().isExhausted());
    }

    private void cleanExpiredConsents() {
        verifiedConsents.entrySet().removeIf(e -> e.getValue().isExpired() || e.getValue().isConsumed());
    }
}
