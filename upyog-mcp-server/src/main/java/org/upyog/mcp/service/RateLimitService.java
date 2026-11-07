package org.upyog.mcp.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.stereotype.Service;
import org.upyog.mcp.config.McpProperties;

import java.time.Duration;

/**
 * Per-user request cap. This limiter is local to the process. Redis covers token single-use, not this counter.
 */
@Service
public class RateLimitService {

    private final RateLimiterRegistry registry;

    public RateLimitService(McpProperties properties) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(properties.getRateLimitPerMinute())
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        this.registry = RateLimiterRegistry.of(config);
    }

    /**
     * Blocks when the per-user minute cap is exceeded.
     *
     * @param userId typically {@link org.upyog.mcp.web.AuthenticatedUser#getUuid()}
     * @throws McpException with code {@code RATE_LIMITED} when denied
     */
    public void acquire(String userId) {
        RateLimiter limiter = registry.rateLimiter(userId);
        if (!limiter.acquirePermission()) {
            throw new McpException("RATE_LIMITED", "Too many requests for this user.",
                    true, "Wait a minute and try again.");
        }
    }
}
