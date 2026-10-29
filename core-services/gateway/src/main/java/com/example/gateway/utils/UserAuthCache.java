package com.example.gateway.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.egov.common.contract.request.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Function;

@Component
public class UserAuthCache {

    private final Cache<String, User> cache;

    public UserAuthCache(@Value("${egov.gateway.user-auth-cache-ttl-seconds:120}") long ttlSeconds) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(10_000)
                .build();
    }

    public User get(String authToken, Function<String, User> loader) {
        return cache.get(authToken, loader);
    }

    public void invalidate(String authToken) {
        if (authToken != null && !authToken.isEmpty()) {
            cache.invalidate(authToken);
        }
    }
}
