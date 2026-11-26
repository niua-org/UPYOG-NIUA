package com.example.gateway.utils;

import org.egov.common.contract.request.User;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthCacheTest {

    @Test
    void get_sameToken_callsLoaderOnce() {
        UserAuthCache cache = new UserAuthCache(60, 100);
        AtomicInteger loads = new AtomicInteger();
        User user = User.builder().uuid("u1").userName("citizen").build();

        User first = cache.get("token-a", token -> {
            loads.incrementAndGet();
            return user;
        });
        User second = cache.get("token-a", token -> {
            loads.incrementAndGet();
            return User.builder().uuid("other").build();
        });

        assertThat(first).isSameAs(user);
        assertThat(second).isSameAs(user);
        assertThat(loads).hasValue(1);
    }

    @Test
    void invalidate_blankToken_returnsFalseAndDoesNotBumpInvalidationCount() {
        UserAuthCache cache = new UserAuthCache(60, 100);
        cache.get("t1", t -> User.builder().uuid("u1").build());

        assertThat(cache.invalidate(null)).isFalse();
        assertThat(cache.invalidate("")).isFalse();
        assertThat(cache.snapshotStats().invalidations()).isZero();
    }

    @Test
    void invalidate_existingToken_evictsEntry() {
        UserAuthCache cache = new UserAuthCache(60, 100);
        AtomicInteger loads = new AtomicInteger();
        cache.get("token-b", token -> {
            loads.incrementAndGet();
            return User.builder().uuid("u1").build();
        });

        assertThat(cache.invalidate("token-b")).isTrue();
        assertThat(cache.snapshotStats().invalidations()).isEqualTo(1);

        cache.get("token-b", token -> {
            loads.incrementAndGet();
            return User.builder().uuid("u2").build();
        });
        assertThat(loads).hasValue(2);
    }

    @Test
    void snapshotStats_reflectsCacheConfiguration() {
        UserAuthCache cache = new UserAuthCache(120, 50);
        cache.get("t", t -> User.builder().uuid("u").build());

        UserAuthCache.StatsSnapshot stats = cache.snapshotStats();
        assertThat(cache.getTtlSeconds()).isEqualTo(120);
        assertThat(cache.getMaximumSize()).isEqualTo(50);
        assertThat(stats.estimatedSize()).isEqualTo(1);
        assertThat(stats.hitRate()).isGreaterThanOrEqualTo(0.0);
    }
}
