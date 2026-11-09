package org.egov.dx.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.service.AadhaarSessionManager;
import org.egov.dx.util.AadhaarConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production implementation of {@link AadhaarSessionManager} supporting distributed Redis tracking
 * with high-performance concurrent in-memory fallback.
 *
 * <p>Adheres strictly to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles rate-limiting counters and replay tracking.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Pure constructor injection without field autowiring.</li>
 *   <li><b>Liskov Substitution Principle (LSP):</b> Seamlessly fulfills the {@link AadhaarSessionManager} contract.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class AadhaarSessionManagerImpl implements AadhaarSessionManager {

    private final int maxOtpAttempts;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Map<String, Integer> otpAttemptsByRefId = new ConcurrentHashMap<>();
    private final Set<String> consumedReferenceIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, String> aadhaarByRefId = new ConcurrentHashMap<>();
    private final Map<String, String> mobileByRefId = new ConcurrentHashMap<>();

    @Autowired
    public AadhaarSessionManagerImpl(@Value("${aadhaar.otp.max.attempts}") int maxOtpAttempts,
                                    @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.maxOtpAttempts = maxOtpAttempts;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void recordFailedOtpAttempt(String refId) {
        if (StringUtils.isBlank(refId)) return;
        if (redisTemplate != null) {
            try {
                String key = AadhaarConstants.REDIS_KEY_OTP_ATTEMPTS_PREFIX + refId;
                Long count = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, Duration.ofMinutes(15));
                if (count != null && count >= maxOtpAttempts) {
                    log.warn("referenceId {} reached max invalid OTP attempts ({}) in Redis. Locked.", refId, maxOtpAttempts);
                }
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for recordFailedOtpAttempt, falling back to in-memory: {}", e.getMessage());
            }
        }
        int attempts = otpAttemptsByRefId.compute(refId, (k, v) -> (v == null ? 1 : v + 1));
        if (attempts >= maxOtpAttempts) {
            log.warn("referenceId {} reached max invalid OTP attempts ({}). Locked.", refId, maxOtpAttempts);
        }
    }

    @Override
    public int getFailedOtpAttempts(String refId) {
        if (StringUtils.isBlank(refId)) return 0;
        if (redisTemplate != null) {
            try {
                Object val = redisTemplate.opsForValue().get(AadhaarConstants.REDIS_KEY_OTP_ATTEMPTS_PREFIX + refId);
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                } else if (val != null) {
                    return Integer.parseInt(val.toString());
                }
            } catch (Exception e) {
                log.warn("Redis read failed for failed attempts, falling back to memory: {}", e.getMessage());
            }
        }
        return otpAttemptsByRefId.getOrDefault(refId, 0);
    }

    @Override
    public void clearFailedOtpAttempts(String refId) {
        if (StringUtils.isBlank(refId)) return;
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(AadhaarConstants.REDIS_KEY_OTP_ATTEMPTS_PREFIX + refId);
            } catch (Exception e) {
                log.warn("Redis delete failed for failed attempts: {}", e.getMessage());
            }
        }
        otpAttemptsByRefId.remove(refId);
    }

    @Override
    public boolean isReferenceIdConsumed(String refId) {
        if (StringUtils.isBlank(refId)) return false;
        if (redisTemplate != null) {
            try {
                Boolean hasKey = redisTemplate.hasKey(AadhaarConstants.REDIS_KEY_OTP_CONSUMED_PREFIX + refId);
                if (Boolean.TRUE.equals(hasKey)) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("Redis lookup failed for consumed reference ID: {}", e.getMessage());
            }
        }
        return consumedReferenceIds.contains(refId);
    }

    @Override
    public void markReferenceIdConsumed(String refId) {
        if (StringUtils.isBlank(refId)) return;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(AadhaarConstants.REDIS_KEY_OTP_CONSUMED_PREFIX + refId,
                        AadhaarConstants.REDIS_VALUE_CONSUMED, Duration.ofMinutes(30));
            } catch (Exception e) {
                log.warn("Redis set failed for consumed reference ID: {}", e.getMessage());
            }
        }
        consumedReferenceIds.add(refId);
    }

    @Override
    public boolean isAttemptLimitExceeded(String refId) {
        if (StringUtils.isBlank(refId)) return false;
        return getFailedOtpAttempts(refId) >= maxOtpAttempts;
    }

    @Override
    public int getMaxOtpAttempts() {
        return maxOtpAttempts;
    }

    @Override
    public void storeAadhaarForReferenceId(String refId, String aadhaarNumber) {
        if (StringUtils.isBlank(refId) || StringUtils.isBlank(aadhaarNumber)) return;
        if (redisTemplate != null) {
            try {
                String key = AadhaarConstants.REDIS_KEY_OTP_AADHAAR_PREFIX + refId;
                redisTemplate.opsForValue().set(key, aadhaarNumber, Duration.ofMinutes(15));
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for storeAadhaarForReferenceId, falling back to in-memory: {}", e.getMessage());
            }
        }
        aadhaarByRefId.put(refId, aadhaarNumber);
    }

    @Override
    public String getAadhaarForReferenceId(String refId) {
        if (StringUtils.isBlank(refId)) return null;
        if (redisTemplate != null) {
            try {
                String key = AadhaarConstants.REDIS_KEY_OTP_AADHAAR_PREFIX + refId;
                Object val = redisTemplate.opsForValue().get(key);
                if (val != null) {
                    return val.toString();
                }
            } catch (Exception e) {
                log.warn("Redis read failed for getAadhaarForReferenceId, falling back to memory: {}", e.getMessage());
            }
        }
        return aadhaarByRefId.get(refId);
    }

    /**
     * Stores the citizen's mobile number keyed by session reference ID in Redis (or in-memory fallback).
     *
     * @param refId session reference ID
     * @param mobileNumber 10-digit mobile number
     */
    @Override
    public void storeMobileForReferenceId(String refId, String mobileNumber) {
        if (StringUtils.isBlank(refId) || StringUtils.isBlank(mobileNumber)) return;
        if (redisTemplate != null) {
            try {
                String key = AadhaarConstants.REDIS_KEY_OTP_MOBILE_PREFIX + refId;
                redisTemplate.opsForValue().set(key, mobileNumber, Duration.ofMinutes(15));
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for storeMobileForReferenceId, falling back to in-memory: {}", e.getMessage());
            }
        }
        mobileByRefId.put(refId, mobileNumber);
    }

    /**
     * Retrieves the citizen's mobile number for the given session reference ID from Redis (or in-memory fallback).
     *
     * @param refId session reference ID
     * @return mobile number or null if expired or not found
     */
    @Override
    public String getMobileForReferenceId(String refId) {
        if (StringUtils.isBlank(refId)) return null;
        if (redisTemplate != null) {
            try {
                String key = AadhaarConstants.REDIS_KEY_OTP_MOBILE_PREFIX + refId;
                Object val = redisTemplate.opsForValue().get(key);
                if (val != null) {
                    return val.toString();
                }
            } catch (Exception e) {
                log.warn("Redis read failed for getMobileForReferenceId, falling back to memory: {}", e.getMessage());
            }
        }
        return mobileByRefId.get(refId);
    }
}
