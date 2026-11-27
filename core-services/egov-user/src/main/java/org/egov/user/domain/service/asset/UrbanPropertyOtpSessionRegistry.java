package org.egov.user.domain.service.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private final ObjectMapper objectMapper;
    private final Map<String, OtpChallenge> activeChallenges = new ConcurrentHashMap<>();
    private final Map<String, VerifiedPropertyConsent> verifiedConsents = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> localOtpSendTimestamps = new ConcurrentHashMap<>();

    /**
     * Primary constructor injecting optional {@link RedisTemplate} and {@link ObjectMapper}
     * for distributed session storage and JSON mapping.
     *
     * @param redisTemplate Spring RedisTemplate for distributed session management (optional)
     * @param objectMapper ObjectMapper for deserializing Redis payloads (optional)
     */
    @Autowired
    public UrbanPropertyOtpSessionRegistry(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
                                          @Autowired(required = false) ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Constructor injecting optional {@link RedisTemplate} for distributed session storage.
     *
     * @param redisTemplate Spring RedisTemplate for distributed session management (optional)
     */
    public UrbanPropertyOtpSessionRegistry(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this(redisTemplate, null);
    }

    /**
     * Default constructor for testing without Redis support.
     */
    public UrbanPropertyOtpSessionRegistry() {
        this(null, null);
    }

    /**
     * Represents an active OTP challenge issued for property linking verification.
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OtpChallenge implements Serializable {
        private static final long serialVersionUID = 1L;

        private String referenceId;
        private String propertyId;
        private String tenantId;
        private String mobileNumber;
        private String callerUuid;
        private long expiresAt;
        private int maxAttempts;
        @Builder.Default
        private AtomicInteger failedAttempts = new AtomicInteger(0);

        @JsonIgnore
        private transient UrbanPropertyOtpSessionRegistry registry;

        @JsonCreator
        public OtpChallenge(
                @JsonProperty("referenceId") String referenceId,
                @JsonProperty("propertyId") String propertyId,
                @JsonProperty("tenantId") String tenantId,
                @JsonProperty("mobileNumber") String mobileNumber,
                @JsonProperty("callerUuid") String callerUuid,
                @JsonProperty("expiresAt") long expiresAt,
                @JsonProperty("maxAttempts") int maxAttempts) {
            this.referenceId = referenceId;
            this.propertyId = propertyId;
            this.tenantId = tenantId;
            this.mobileNumber = mobileNumber;
            this.callerUuid = callerUuid;
            this.expiresAt = expiresAt;
            this.maxAttempts = maxAttempts;
            this.failedAttempts = new AtomicInteger(0);
        }

        public static OtpChallenge fromMap(Map<String, Object> map) {
            if (map == null) {
                return null;
            }
            long expiresAt = 0L;
            if (map.get("expiresAt") instanceof Number) {
                expiresAt = ((Number) map.get("expiresAt")).longValue();
            }
            int maxAttempts = 3;
            if (map.get("maxAttempts") instanceof Number) {
                maxAttempts = ((Number) map.get("maxAttempts")).intValue();
            }
            return OtpChallenge.builder()
                    .referenceId((String) map.get("referenceId"))
                    .propertyId((String) map.get("propertyId"))
                    .tenantId((String) map.get("tenantId"))
                    .mobileNumber((String) map.get("mobileNumber"))
                    .callerUuid((String) map.get("callerUuid"))
                    .expiresAt(expiresAt)
                    .maxAttempts(maxAttempts)
                    .build();
        }

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
         * Delegates to the registry to update distributed Redis counter if available.
         *
         * @return the newly incremented total count of failed attempts.
         */
        public int recordFailedAttempt() {
            if (registry != null) {
                return registry.recordFailedAttempt(referenceId);
            }
            return recordLocalFailedAttempt();
        }

        /**
         * Atomically increments local in-memory counter.
         */
        public int recordLocalFailedAttempt() {
            return failedAttempts.incrementAndGet();
        }

        /**
         * Returns the current failed attempts count, checking Redis if registry is bound.
         */
        public int getFailedAttemptsCount() {
            if (registry != null) {
                return registry.getFailedAttempts(referenceId);
            }
            return failedAttempts.get();
        }

        /**
         * Returns the local in-memory count of failed attempts without delegating to registry.
         */
        public int getLocalFailedAttempts() {
            return failedAttempts != null ? failedAttempts.get() : 0;
        }

        /**
         * Backward-compatible getter for failedAttempts AtomicInteger.
         */
        public AtomicInteger getFailedAttempts() {
            return failedAttempts;
        }

        /**
         * Checks whether the count of recorded failed verification attempts has reached or exceeded maxAttempts.
         *
         * @return {@code true} if failed attempts &gt;= maxAttempts, {@code false} otherwise.
         */
        public boolean isExhausted() {
            return getFailedAttemptsCount() >= maxAttempts;
        }
    }

    /**
     * Represents an authenticated consent token issued upon successful OTP verification.
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VerifiedPropertyConsent implements Serializable {
        private static final long serialVersionUID = 1L;

        private String consentToken;
        private String propertyId;
        private String tenantId;
        private String mobileNumber;
        private String ownerUuid;
        private String callerUuid;
        private long verifiedTime;
        private long expiresAt;
        @Builder.Default
        private boolean consumed = false;

        @JsonIgnore
        private transient UrbanPropertyOtpSessionRegistry registry;

        @JsonCreator
        public VerifiedPropertyConsent(
                @JsonProperty("consentToken") String consentToken,
                @JsonProperty("propertyId") String propertyId,
                @JsonProperty("tenantId") String tenantId,
                @JsonProperty("mobileNumber") String mobileNumber,
                @JsonProperty("ownerUuid") String ownerUuid,
                @JsonProperty("callerUuid") String callerUuid,
                @JsonProperty("verifiedTime") long verifiedTime,
                @JsonProperty("expiresAt") long expiresAt,
                @JsonProperty("consumed") boolean consumed) {
            this.consentToken = consentToken;
            this.propertyId = propertyId;
            this.tenantId = tenantId;
            this.mobileNumber = mobileNumber;
            this.ownerUuid = ownerUuid;
            this.callerUuid = callerUuid;
            this.verifiedTime = verifiedTime;
            this.expiresAt = expiresAt;
            this.consumed = consumed;
        }

        public static VerifiedPropertyConsent fromMap(Map<String, Object> map) {
            if (map == null) {
                return null;
            }
            long verifiedTime = 0L;
            if (map.get("verifiedTime") instanceof Number) {
                verifiedTime = ((Number) map.get("verifiedTime")).longValue();
            }
            long expiresAt = 0L;
            if (map.get("expiresAt") instanceof Number) {
                expiresAt = ((Number) map.get("expiresAt")).longValue();
            }
            boolean consumed = false;
            if (map.get("consumed") instanceof Boolean) {
                consumed = (Boolean) map.get("consumed");
            }
            return VerifiedPropertyConsent.builder()
                    .consentToken((String) map.get("consentToken"))
                    .propertyId((String) map.get("propertyId"))
                    .tenantId((String) map.get("tenantId"))
                    .mobileNumber((String) map.get("mobileNumber"))
                    .ownerUuid((String) map.get("ownerUuid"))
                    .callerUuid((String) map.get("callerUuid"))
                    .verifiedTime(verifiedTime)
                    .expiresAt(expiresAt)
                    .consumed(consumed)
                    .build();
        }

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
            if (registry != null) {
                registry.markConsentConsumed(consentToken);
            }
        }
    }

    private String getAttemptsRedisKey(String referenceId) {
        return UserConstants.REDIS_PROP_OTP_ATTEMPTS_PREFIX + referenceId.trim();
    }

    @SuppressWarnings("unchecked")
    private OtpChallenge convertToOtpChallenge(Object cached) {
        if (cached == null) {
            return null;
        }
        if (cached instanceof OtpChallenge) {
            return (OtpChallenge) cached;
        }
        if (cached instanceof Map) {
            return OtpChallenge.fromMap((Map<String, Object>) cached);
        }
        try {
            if (objectMapper != null) {
                return objectMapper.convertValue(cached, OtpChallenge.class);
            }
        } catch (Exception e) {
            log.warn("[PROPERTY-OTP-REGISTRY] Failed to deserialize challenge from Redis: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private VerifiedPropertyConsent convertToConsent(Object cached) {
        if (cached == null) {
            return null;
        }
        if (cached instanceof VerifiedPropertyConsent) {
            return (VerifiedPropertyConsent) cached;
        }
        if (cached instanceof Map) {
            return VerifiedPropertyConsent.fromMap((Map<String, Object>) cached);
        }
        try {
            if (objectMapper != null) {
                return objectMapper.convertValue(cached, VerifiedPropertyConsent.class);
            }
        } catch (Exception e) {
            log.warn("[PROPERTY-OTP-REGISTRY] Failed to deserialize consent from Redis: {}", e.getMessage());
        }
        return null;
    }

    private String getRateLimitCallerKey(String callerUuid, String propertyId) {
        String caller = StringUtils.isNotBlank(callerUuid) ? callerUuid.trim() : "ANONYMOUS";
        String prop = StringUtils.isNotBlank(propertyId) ? propertyId.trim().toUpperCase() : "UNKNOWN";
        return UserConstants.REDIS_PROP_OTP_RATE_PREFIX + "CALLER:" + caller + ":" + prop;
    }

    private String getRateLimitMobileKey(String mobileNumber) {
        String mobile = StringUtils.isNotBlank(mobileNumber) ? mobileNumber.trim() : "UNKNOWN";
        return UserConstants.REDIS_PROP_OTP_RATE_PREFIX + "MOBILE:" + mobile;
    }

    private String getRateLimitPropertyKey(String propertyId) {
        String prop = StringUtils.isNotBlank(propertyId) ? propertyId.trim().toUpperCase() : "UNKNOWN";
        return UserConstants.REDIS_PROP_OTP_RATE_PREFIX + "PROPERTY:" + prop;
    }

    /**
     * Checks whether an OTP dispatch request is allowed under multi-dimensional rate-limiting policies:
     * 1. Per (callerUuid, propertyId): max 3 dispatches per 15-minute sliding window to prevent malicious callers from locking out owners.
     * 2. Per destination mobile: max 3 dispatches per 15-minute sliding window to prevent SMS flooding.
     * 3. Per property: looser global ceiling (10 dispatches per 15-minute window) across all callers to mitigate distributed attacks.
     *
     * @param callerUuid citizen caller UUID initiating the dispatch (optional/anonymous if null)
     * @param propertyId municipal property identifier
     * @param mobileNumber destination mobile number receiving the OTP (optional)
     * @return {@code true} if dispatch is permitted, {@code false} if rate-limited
     */
    public boolean isOtpDispatchAllowed(String callerUuid, String propertyId, String mobileNumber) {
        if (StringUtils.isBlank(propertyId)) {
            return false;
        }
        String callerKey = getRateLimitCallerKey(callerUuid, propertyId);
        String propKey = getRateLimitPropertyKey(propertyId);
        String mobileKey = StringUtils.isNotBlank(mobileNumber) ? getRateLimitMobileKey(mobileNumber) : null;

        if (redisTemplate != null) {
            try {
                Object cVal = redisTemplate.opsForValue().get(callerKey);
                if (cVal instanceof Number && ((Number) cVal).intValue() >= UserConstants.MAX_PROPERTY_OTP_SENDS_PER_CALLER) {
                    return false;
                }
                if (mobileKey != null) {
                    Object mVal = redisTemplate.opsForValue().get(mobileKey);
                    if (mVal instanceof Number && ((Number) mVal).intValue() >= UserConstants.MAX_PROPERTY_OTP_SENDS_PER_MOBILE) {
                        return false;
                    }
                }
                Object pVal = redisTemplate.opsForValue().get(propKey);
                if (pVal instanceof Number && ((Number) pVal).intValue() >= UserConstants.MAX_PROPERTY_OTP_SENDS_PER_PROPERTY_CEILING) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis rate limit check failed, falling back to local: {}", e.getMessage());
            }
        }

        long now = System.currentTimeMillis();
        long windowStart = now - UserConstants.PROPERTY_OTP_SEND_WINDOW_MS;
        synchronized (localOtpSendTimestamps) {
            List<Long> callerTs = localOtpSendTimestamps.get(callerKey);
            if (callerTs != null) {
                callerTs.removeIf(ts -> ts < windowStart);
                if (callerTs.size() >= UserConstants.MAX_PROPERTY_OTP_SENDS_PER_CALLER) {
                    return false;
                }
            }
            if (mobileKey != null) {
                List<Long> mobileTs = localOtpSendTimestamps.get(mobileKey);
                if (mobileTs != null) {
                    mobileTs.removeIf(ts -> ts < windowStart);
                    if (mobileTs.size() >= UserConstants.MAX_PROPERTY_OTP_SENDS_PER_MOBILE) {
                        return false;
                    }
                }
            }
            List<Long> propTs = localOtpSendTimestamps.get(propKey);
            if (propTs != null) {
                propTs.removeIf(ts -> ts < windowStart);
                if (propTs.size() >= UserConstants.MAX_PROPERTY_OTP_SENDS_PER_PROPERTY_CEILING) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Backward-compatible rate limit check for a single propertyId parameter without caller or mobile context.
     * Delegates to {@link #isOtpDispatchAllowed(String, String, String)} with null callerUuid and mobileNumber.
     *
     * @param propertyId municipal property identifier
     * @return true if OTP dispatch is permitted under property ceiling, false otherwise
     */
    public boolean isOtpDispatchAllowed(String propertyId) {
        return isOtpDispatchAllowed(null, propertyId, null);
    }

    /**
     * Atomically reserves an OTP dispatch slot across (callerUuid, propertyId), destination mobile,
     * and looser property ceiling BEFORE dispatching the OTP.
     * In Redis: uses atomic INCR with EXPIRE to share rate limits across pods without race conditions.
     * In local memory: performs synchronized check-and-add to eliminate check-then-act gaps.
     *
     * @param callerUuid citizen caller UUID initiating the dispatch
     * @param propertyId municipal property identifier
     * @param mobileNumber destination mobile number receiving the OTP
     * @return {@code true} if slot was reserved successfully, {@code false} if rate-limited
     */
    public boolean tryReserveOtpDispatchSlot(String callerUuid, String propertyId, String mobileNumber) {
        if (StringUtils.isBlank(propertyId)) {
            return false;
        }
        String callerKey = getRateLimitCallerKey(callerUuid, propertyId);
        String propKey = getRateLimitPropertyKey(propertyId);
        String mobileKey = StringUtils.isNotBlank(mobileNumber) ? getRateLimitMobileKey(mobileNumber) : null;

        if (redisTemplate != null) {
            try {
                // 1. Reserve caller-property slot
                Long cCount = redisTemplate.opsForValue().increment(callerKey);
                if (cCount != null && cCount == 1) {
                    redisTemplate.expire(callerKey, UserConstants.PROPERTY_OTP_SEND_WINDOW_MS, TimeUnit.MILLISECONDS);
                }
                if (cCount != null && cCount > UserConstants.MAX_PROPERTY_OTP_SENDS_PER_CALLER) {
                    return false;
                }

                // 2. Reserve mobile slot
                Long mCount = null;
                if (mobileKey != null) {
                    mCount = redisTemplate.opsForValue().increment(mobileKey);
                    if (mCount != null && mCount == 1) {
                        redisTemplate.expire(mobileKey, UserConstants.PROPERTY_OTP_SEND_WINDOW_MS, TimeUnit.MILLISECONDS);
                    }
                    if (mCount != null && mCount > UserConstants.MAX_PROPERTY_OTP_SENDS_PER_MOBILE) {
                        redisTemplate.opsForValue().decrement(callerKey);
                        return false;
                    }
                }

                // 3. Reserve property ceiling slot
                Long pCount = redisTemplate.opsForValue().increment(propKey);
                if (pCount != null && pCount == 1) {
                    redisTemplate.expire(propKey, UserConstants.PROPERTY_OTP_SEND_WINDOW_MS, TimeUnit.MILLISECONDS);
                }
                if (pCount != null && pCount > UserConstants.MAX_PROPERTY_OTP_SENDS_PER_PROPERTY_CEILING) {
                    redisTemplate.opsForValue().decrement(callerKey);
                    if (mobileKey != null) {
                        redisTemplate.opsForValue().decrement(mobileKey);
                    }
                    return false;
                }

                return true;
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis rate limit reservation failed, falling back to local: {}", e.getMessage());
            }
        }

        // Local in-memory atomic reservation
        long now = System.currentTimeMillis();
        long windowStart = now - UserConstants.PROPERTY_OTP_SEND_WINDOW_MS;
        synchronized (localOtpSendTimestamps) {
            List<Long> callerTs = localOtpSendTimestamps.computeIfAbsent(callerKey, k -> new ArrayList<>());
            callerTs.removeIf(ts -> ts < windowStart);
            if (callerTs.size() >= UserConstants.MAX_PROPERTY_OTP_SENDS_PER_CALLER) {
                return false;
            }

            List<Long> mobileTs = null;
            if (mobileKey != null) {
                mobileTs = localOtpSendTimestamps.computeIfAbsent(mobileKey, k -> new ArrayList<>());
                mobileTs.removeIf(ts -> ts < windowStart);
                if (mobileTs.size() >= UserConstants.MAX_PROPERTY_OTP_SENDS_PER_MOBILE) {
                    return false;
                }
            }

            List<Long> propTs = localOtpSendTimestamps.computeIfAbsent(propKey, k -> new ArrayList<>());
            propTs.removeIf(ts -> ts < windowStart);
            if (propTs.size() >= UserConstants.MAX_PROPERTY_OTP_SENDS_PER_PROPERTY_CEILING) {
                return false;
            }

            // Atomically reserve slots inside synchronized block
            callerTs.add(now);
            if (mobileTs != null) {
                mobileTs.add(now);
            }
            propTs.add(now);
            return true;
        }
    }

    /**
     * Releases a previously reserved OTP dispatch slot in the event of downstream dispatch failure.
     *
     * @param callerUuid citizen caller UUID initiating the dispatch
     * @param propertyId municipal property identifier
     * @param mobileNumber destination mobile number receiving the OTP
     */
    public void releaseOtpDispatchSlot(String callerUuid, String propertyId, String mobileNumber) {
        if (StringUtils.isBlank(propertyId)) {
            return;
        }
        String callerKey = getRateLimitCallerKey(callerUuid, propertyId);
        String propKey = getRateLimitPropertyKey(propertyId);
        String mobileKey = StringUtils.isNotBlank(mobileNumber) ? getRateLimitMobileKey(mobileNumber) : null;

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().decrement(callerKey);
                if (mobileKey != null) {
                    redisTemplate.opsForValue().decrement(mobileKey);
                }
                redisTemplate.opsForValue().decrement(propKey);
                return;
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis rate limit release failed: {}", e.getMessage());
            }
        }

        synchronized (localOtpSendTimestamps) {
            List<Long> callerTs = localOtpSendTimestamps.get(callerKey);
            if (callerTs != null && !callerTs.isEmpty()) {
                callerTs.remove(callerTs.size() - 1);
            }
            if (mobileKey != null) {
                List<Long> mobileTs = localOtpSendTimestamps.get(mobileKey);
                if (mobileTs != null && !mobileTs.isEmpty()) {
                    mobileTs.remove(mobileTs.size() - 1);
                }
            }
            List<Long> propTs = localOtpSendTimestamps.get(propKey);
            if (propTs != null && !propTs.isEmpty()) {
                propTs.remove(propTs.size() - 1);
            }
        }
    }

    /**
     * Records an OTP dispatch event in the rate-limiting tracking registry.
     *
     * @param propertyId municipal property identifier
     */
    public void recordOtpDispatch(String propertyId) {
        tryReserveOtpDispatchSlot(null, propertyId, null);
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
        challenge.setRegistry(this);

        activeChallenges.put(referenceId, challenge);

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(UserConstants.REDIS_PROP_OTP_CHALLENGE_PREFIX + referenceId, challenge, ttlMs, TimeUnit.MILLISECONDS);
                redisTemplate.delete(getAttemptsRedisKey(referenceId));
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis write failed for challenge {}: {}", MaskUtils.maskIdentifier(referenceId), e.getMessage());
            }
        }

        log.info("[PROPERTY-OTP-REGISTRY] Registered challenge: {} for property: {}, mobile: {}",
                MaskUtils.maskIdentifier(referenceId), propertyId, MaskUtils.maskMobile(mobileNumber));
    }

    /**
     * Backward-compatible challenge registration without callerUuid binding.
     * Delegates to {@link #registerChallenge(String, String, String, String, String, long, int)} with null callerUuid.
     *
     * @param referenceId tracking reference identifier
     * @param propertyId municipal property identifier
     * @param tenantId tenant identifier
     * @param mobileNumber property owner mobile number
     * @param ttlMs session duration in milliseconds
     * @param maxAttempts maximum allowed failed verification attempts
     */
    public void registerChallenge(String referenceId, String propertyId, String tenantId,
                                  String mobileNumber, long ttlMs, int maxAttempts) {
        registerChallenge(referenceId, propertyId, tenantId, mobileNumber, null, ttlMs, maxAttempts);
    }

    /**
     * Records a failed attempt for the specified challenge reference.
     * Increments the Redis attempt counter with INCR and sets TTL on first failure.
     *
     * @param referenceId tracking reference identifier
     * @return current count of failed attempts
     */
    public int recordFailedAttempt(String referenceId) {
        if (StringUtils.isBlank(referenceId)) {
            return 0;
        }
        String ref = referenceId.trim();
        int attempts = 0;
        if (redisTemplate != null) {
            try {
                String attemptsKey = getAttemptsRedisKey(ref);
                Long count = redisTemplate.opsForValue().increment(attemptsKey);
                if (count != null) {
                    attempts = count.intValue();
                    if (count == 1) {
                        OtpChallenge challenge = resolveActiveChallenge(ref, null, null);
                        long ttlMs = challenge != null ? (challenge.getExpiresAt() - System.currentTimeMillis()) : UserConstants.PROPERTY_OTP_SEND_WINDOW_MS;
                        if (ttlMs > 0) {
                            redisTemplate.expire(attemptsKey, ttlMs, TimeUnit.MILLISECONDS);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis increment failed for attempts on {}: {}", MaskUtils.maskIdentifier(ref), e.getMessage());
            }
        }

        OtpChallenge local = activeChallenges.get(ref);
        if (local != null) {
            int localCount = local.recordLocalFailedAttempt();
            if (attempts == 0) {
                attempts = localCount;
            }
        }
        return attempts;
    }

    /**
     * Returns the current failed attempt count for the specified challenge reference.
     *
     * @param referenceId tracking reference identifier
     * @return count of failed attempts recorded
     */
    public int getFailedAttempts(String referenceId) {
        if (StringUtils.isBlank(referenceId)) {
            return 0;
        }
        String ref = referenceId.trim();
        if (redisTemplate != null) {
            try {
                Object val = redisTemplate.opsForValue().get(getAttemptsRedisKey(ref));
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                } else if (val != null) {
                    return Integer.parseInt(val.toString().trim());
                }
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis read failed for attempts on {}: {}", MaskUtils.maskIdentifier(ref), e.getMessage());
            }
        }
        OtpChallenge local = activeChallenges.get(ref);
        return local != null ? local.getLocalFailedAttempts() : 0;
    }

    /**
     * Resolves an unexpired, unexhausted challenge matching reference ID, property ID, and tenant.
     *
     * @param referenceId tracking reference identifier
     * @param propertyId municipal property identifier (optional check if non-blank)
     * @param tenantId tenant identifier (optional check if non-blank)
     * @return active OtpChallenge if valid, or null
     */
    public OtpChallenge resolveActiveChallenge(String referenceId, String propertyId, String tenantId) {
        cleanExpiredChallenges();
        if (StringUtils.isBlank(referenceId)) {
            return null;
        }

        OtpChallenge challenge = null;
        boolean redisQueried = false;
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(UserConstants.REDIS_PROP_OTP_CHALLENGE_PREFIX + referenceId.trim());
                challenge = convertToOtpChallenge(cached);
                redisQueried = true;
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis read failed for challenge {}: {}", MaskUtils.maskIdentifier(referenceId), e.getMessage());
            }
        }

        if (challenge == null) {
            if (!redisQueried) {
                challenge = activeChallenges.get(referenceId.trim());
            } else {
                activeChallenges.remove(referenceId.trim());
            }
        }

        if (challenge == null) {
            return null;
        }
        challenge.setRegistry(this);

        if (challenge.isExpired() || challenge.isExhausted()) {
            invalidateChallenge(referenceId);
            return null;
        }
        if (StringUtils.isNotBlank(propertyId) && !challenge.getPropertyId().equalsIgnoreCase(propertyId.trim())) {
            log.warn("[PROPERTY-OTP-REGISTRY] Mismatched challenge details for reference: {}", MaskUtils.maskIdentifier(referenceId));
            return null;
        }
        if (StringUtils.isNotBlank(tenantId) && !challenge.getTenantId().equalsIgnoreCase(tenantId.trim())) {
            log.warn("[PROPERTY-OTP-REGISTRY] Mismatched challenge details for reference: {}", MaskUtils.maskIdentifier(referenceId));
            return null;
        }
        return challenge;
    }

    /**
     * Invalidates and removes an active OTP challenge and its attempt counter from the registry.
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
                    redisTemplate.delete(getAttemptsRedisKey(key));
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
        consent.setRegistry(this);

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
     * Resolves a consent token from Redis or local map.
     */
    private VerifiedPropertyConsent resolveConsent(String consentToken) {
        if (StringUtils.isBlank(consentToken)) {
            return null;
        }
        String key = consentToken.trim();
        VerifiedPropertyConsent consent = null;
        boolean redisQueried = false;
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(UserConstants.REDIS_PROP_CONSENT_PREFIX + key);
                consent = convertToConsent(cached);
                redisQueried = true;
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis read failed for consent {}: {}", MaskUtils.maskIdentifier(key), e.getMessage());
            }
        }
        if (consent == null) {
            if (!redisQueried) {
                consent = verifiedConsents.get(key);
            } else {
                verifiedConsents.remove(key);
            }
        }
        if (consent != null) {
            consent.setRegistry(this);
        }
        return consent;
    }

    /**
     * Marks a consent token as consumed and removes it from Redis and local registry.
     *
     * @param consentToken issued consent token string
     */
    public void markConsentConsumed(String consentToken) {
        if (StringUtils.isBlank(consentToken)) {
            return;
        }
        String key = consentToken.trim();
        VerifiedPropertyConsent local = verifiedConsents.get(key);
        if (local != null) {
            local.setConsumed(true);
        }
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(UserConstants.REDIS_PROP_CONSENT_PREFIX + key);
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis markConsumed failed for consent {}: {}", MaskUtils.maskIdentifier(key), e.getMessage());
            }
        }
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
     * Backward-compatible consent token validation without caller UUID and tenant binding checks.
     * Delegates to {@link #validateConsentToken(String, String, String, String)} with null callerUuid and tenantId.
     *
     * @param propertyId municipal property identifier
     * @param consentToken issued consent token string
     * @return true if valid and active, false otherwise
     */
    public boolean validateConsentToken(String propertyId, String consentToken) {
        return validateConsentToken(propertyId, consentToken, null, null);
    }

    /**
     * Atomically consumes (retrieves and removes) a single-use consent token, ensuring it belongs to the
     * target propertyId, callerUuid, and tenantId.
     * Uses atomic GETDEL (opsForValue().getAndDelete) in Redis and atomic remove(key) locally,
     * validating the returned object only after removal to prevent race conditions and concurrent double-consumption.
     *
     * @param propertyId municipal property identifier
     * @param consentToken issued consent token string
     * @param callerUuid citizen caller UUID attempting to link the property
     * @param tenantId tenant identifier
     * @return true if successfully consumed, false otherwise
     */
    public boolean consumeConsentToken(String propertyId, String consentToken, String callerUuid, String tenantId) {
        if (StringUtils.isBlank(propertyId) || StringUtils.isBlank(consentToken)) {
            return false;
        }
        String key = consentToken.trim();
        VerifiedPropertyConsent consent = null;
        boolean redisQueried = false;

        // Atomically retrieve and delete token using Redis GETDEL (opsForValue().getAndDelete)
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().getAndDelete(UserConstants.REDIS_PROP_CONSENT_PREFIX + key);
                consent = convertToConsent(cached);
                redisQueried = true;
            } catch (Exception e) {
                log.warn("[PROPERTY-OTP-REGISTRY] Redis getAndDelete failed for consent {}: {}", MaskUtils.maskIdentifier(key), e.getMessage());
            }
        }

        // Atomically retrieve and remove from in-memory map
        if (consent == null) {
            if (!redisQueried) {
                consent = verifiedConsents.remove(key);
            } else {
                verifiedConsents.remove(key);
            }
        } else {
            VerifiedPropertyConsent local = verifiedConsents.remove(key);
            if (local != null) {
                local.markConsumed();
            }
        }

        // If no token was found and removed, it was either never registered or already consumed
        if (consent == null) {
            return false;
        }

        consent.setRegistry(this);

        // Validate the removed token
        if (consent.isExpired() || consent.isConsumed()) {
            return false;
        }
        if (!consent.getPropertyId().equalsIgnoreCase(propertyId.trim())) {
            log.warn("[PROPERTY-OTP-REGISTRY] Property ID mismatch during token consumption: expected {}, got {}",
                    consent.getPropertyId(), propertyId);
            return false;
        }
        if (StringUtils.isNotBlank(tenantId) && StringUtils.isNotBlank(consent.getTenantId())
                && !consent.getTenantId().equalsIgnoreCase(tenantId.trim())) {
            log.warn("[PROPERTY-OTP-REGISTRY] Tenant ID mismatch during token consumption: expected {}, got {}",
                    consent.getTenantId(), tenantId);
            return false;
        }
        if (StringUtils.isNotBlank(callerUuid) && StringUtils.isNotBlank(consent.getCallerUuid())
                && !consent.getCallerUuid().equals(callerUuid.trim())) {
            log.warn("[PROPERTY-OTP-REGISTRY] Caller UUID mismatch for consent {}: expected {}, got {}",
                    MaskUtils.maskIdentifier(key), consent.getCallerUuid(), callerUuid);
            return false;
        }

        consent.markConsumed();
        return true;
    }


    private void cleanExpiredChallenges() {
        activeChallenges.entrySet().removeIf(e -> e.getValue().isExpired() || e.getValue().isExhausted());
    }

    private void cleanExpiredConsents() {
        verifiedConsents.entrySet().removeIf(e -> e.getValue().isExpired() || e.getValue().isConsumed());
    }
}
