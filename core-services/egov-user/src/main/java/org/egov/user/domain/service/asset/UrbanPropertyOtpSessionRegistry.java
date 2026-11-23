package org.egov.user.domain.service.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.util.MaskUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory thread-safe session registry for managing Urban Property OTP challenges
 * and verified property consent tokens.
 */
@Component
@Slf4j
public class UrbanPropertyOtpSessionRegistry {

    private final Map<String, OtpChallenge> activeChallenges = new ConcurrentHashMap<>();
    private final Map<String, VerifiedPropertyConsent> verifiedConsents = new ConcurrentHashMap<>();

    /**
     * Represents an active OTP challenge issued for property linking verification.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class OtpChallenge {
        private final String referenceId;
        private final String propertyId;
        private final String tenantId;
        private final String mobileNumber;
        private final long expiresAt;
        private final int maxAttempts;
        @Builder.Default
        private final AtomicInteger failedAttempts = new AtomicInteger(0);

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        public int recordFailedAttempt() {
            return failedAttempts.incrementAndGet();
        }

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
    public static class VerifiedPropertyConsent {
        private final String consentToken;
        private final String propertyId;
        private final String tenantId;
        private final String mobileNumber;
        private final String ownerUuid;
        private final long verifiedTime;
        private final long expiresAt;

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * Registers a new active OTP challenge for property ownership verification.
     *
     * @param referenceId tracking reference identifier
     * @param propertyId municipal property identifier
     * @param tenantId tenant identifier
     * @param mobileNumber destination mobile number receiving the OTP
     * @param ttlMs session duration in milliseconds
     * @param maxAttempts maximum allowed failed verification attempts
     */
    public void registerChallenge(String referenceId, String propertyId, String tenantId,
                                  String mobileNumber, long ttlMs, int maxAttempts) {
        cleanExpiredChallenges();
        long now = System.currentTimeMillis();
        OtpChallenge challenge = OtpChallenge.builder()
                .referenceId(referenceId)
                .propertyId(propertyId.trim())
                .tenantId(tenantId.trim())
                .mobileNumber(mobileNumber.trim())
                .expiresAt(now + ttlMs)
                .maxAttempts(maxAttempts)
                .build();
        activeChallenges.put(referenceId, challenge);
        log.info("[PROPERTY-OTP-REGISTRY] Registered challenge: {} for property: {}, mobile: {}",
                MaskUtils.maskIdentifier(referenceId), propertyId, MaskUtils.maskMobile(mobileNumber));
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
        OtpChallenge challenge = activeChallenges.get(referenceId.trim());
        if (challenge == null) {
            return null;
        }
        if (challenge.isExpired() || challenge.isExhausted()) {
            activeChallenges.remove(referenceId);
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
            activeChallenges.remove(referenceId.trim());
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
     * @param ttlMs token validity duration in milliseconds
     */
    public void registerConsentToken(String consentToken, String propertyId, String tenantId,
                                     String mobileNumber, String ownerUuid, long ttlMs) {
        cleanExpiredConsents();
        long now = System.currentTimeMillis();
        VerifiedPropertyConsent consent = VerifiedPropertyConsent.builder()
                .consentToken(consentToken)
                .propertyId(propertyId.trim())
                .tenantId(tenantId.trim())
                .mobileNumber(mobileNumber != null ? mobileNumber.trim() : null)
                .ownerUuid(ownerUuid)
                .verifiedTime(now)
                .expiresAt(now + ttlMs)
                .build();
        verifiedConsents.put(consentToken, consent);
        log.info("[PROPERTY-OTP-REGISTRY] Registered verified property consent token: {} for property: {}",
                MaskUtils.maskIdentifier(consentToken), propertyId);
    }

    /**
     * Checks if there is any unexpired verified consent for the given propertyId.
     * Allows linking validation when the client does not explicitly supply a consentToken in the payload.
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
                .anyMatch(c -> c.getPropertyId().equalsIgnoreCase(propertyId.trim()) && !c.isExpired());
    }

    /**
     * Validates whether a consent token exists, is unexpired, and belongs to the given propertyId.
     *
     * @param propertyId municipal property identifier
     * @param consentToken issued consent token string
     * @return true if valid and active, false otherwise
     */
    public boolean validateConsentToken(String propertyId, String consentToken) {
        cleanExpiredConsents();
        if (StringUtils.isBlank(propertyId) || StringUtils.isBlank(consentToken)) {
            return false;
        }
        VerifiedPropertyConsent consent = verifiedConsents.get(consentToken.trim());
        if (consent == null) {
            return false;
        }
        if (consent.isExpired()) {
            verifiedConsents.remove(consentToken.trim());
            return false;
        }
        return consent.getPropertyId().equalsIgnoreCase(propertyId.trim());
    }

    /**
     * Validates and consumes (removes) a single-use consent token.
     *
     * @param propertyId municipal property identifier
     * @param consentToken issued consent token string
     * @return true if successfully consumed, false otherwise
     */
    public boolean consumeConsentToken(String propertyId, String consentToken) {
        if (validateConsentToken(propertyId, consentToken)) {
            verifiedConsents.remove(consentToken.trim());
            return true;
        }
        return false;
    }

    private void cleanExpiredChallenges() {
        activeChallenges.entrySet().removeIf(e -> e.getValue().isExpired() || e.getValue().isExhausted());
    }

    private void cleanExpiredConsents() {
        verifiedConsents.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
