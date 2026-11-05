package com.example.gateway.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.egov.common.contract.request.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * In-memory cache of auth-token → {@link User} from {@code user/_details}.
 * Reduces repeated blocking HTTP calls during a request burst. Entries expire after a short TTL;
 * {@link com.example.gateway.filters.post.CustomAsyncFilter} invalidates the token on {@code /user/_logout}.
 */
@Component
public class UserAuthCache {

    private final Cache<String, User> cache;
    private final AtomicLong invalidations = new AtomicLong();
    private final long ttlSeconds;
    private final long maximumSize;

    public UserAuthCache(
            @Value("${egov.gateway.user-auth-cache-ttl-seconds}") long ttlSeconds,
            @Value("${egov.gateway.user-auth-cache-max-size}") long maximumSize) {
        this.ttlSeconds = ttlSeconds;
        this.maximumSize = maximumSize;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maximumSize)
                .recordStats()
                .build();
    }

    public User get(String authToken, Function<String, User> loader) {
        return cache.get(authToken, loader);
    }

    public boolean invalidate(String authToken) {
        if (authToken != null && !authToken.isEmpty()) {
            cache.invalidate(authToken);
            invalidations.incrementAndGet();
            return true;
        }
        return false;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public long getMaximumSize() {
        return maximumSize;
    }

    public StatsSnapshot snapshotStats() {
        CacheStats stats = cache.stats();
        return new StatsSnapshot(
                cache.estimatedSize(),
                stats.hitRate(),
                stats.evictionCount(),
                invalidations.get());
    }

    public record StatsSnapshot(long estimatedSize, double hitRate, long evictionCount, long invalidations) {
    }
}
