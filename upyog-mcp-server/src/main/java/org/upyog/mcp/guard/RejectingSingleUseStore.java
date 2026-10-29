package org.upyog.mcp.guard;

import java.time.Duration;
import java.util.Optional;

/**
 * Fail-closed store used when Redis is disabled.
 * {@link #writesEnabled()} is false so confirm cannot pretend that local memory is single-use across pods.
 */
public class RejectingSingleUseStore implements SingleUseStore {

    /** Always throws; confirm must not run without Redis. */
    @Override
    public boolean consumeOnce(String key, Duration ttl) {
        throw new IllegalStateException("Redis is required for single-use confirmation tokens");
    }

    @Override
    public void saveResult(String key, String json, Duration ttl) {
        throw new IllegalStateException("Redis is required for idempotent writes");
    }

    @Override
    public Optional<String> findResult(String key) {
        return Optional.empty();
    }

    @Override
    public boolean writesEnabled() {
        return false;
    }
}
