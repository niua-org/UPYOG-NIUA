package org.egov.dx.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.service.AadhaarSessionManager;
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
                String key = "aadhaar:otp:attempts:" + refId;
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
                Object val = redisTemplate.opsForValue().get("aadhaar:otp:attempts:" + refId);
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
                redisTemplate.delete("aadhaar:otp:attempts:" + refId);
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
                Boolean hasKey = redisTemplate.hasKey("aadhaar:otp:consumed:" + refId);
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
                redisTemplate.opsForValue().set("aadhaar:otp:consumed:" + refId, "CONSUMED", Duration.ofMinutes(30));
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
}
