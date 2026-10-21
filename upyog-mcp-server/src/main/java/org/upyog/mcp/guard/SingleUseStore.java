package org.upyog.mcp.guard;

import java.time.Duration;
import java.util.Optional;

/**
 * Shared store for one-time confirmation ids and idempotent write results.
 * Production uses Redis. A process-local map is not a valid implementation of this contract.
 */
public interface SingleUseStore {

    /**
     * Marks a key as used exactly once (for example {@code confirm:<jti>}).
     *
     * @return {@code true} when this call won the race; {@code false} when the key already exists
     */
    boolean consumeOnce(String key, Duration ttl);

    /** Stores a serialized tool result for idempotent replay of the same confirmed write. */
    void saveResult(String key, String json, Duration ttl);

    /** Returns a previously stored write result when the same user and payload hash confirm again. */
    Optional<String> findResult(String key);

    /** @return {@code false} when confirm must fail closed (Redis disabled). */
    boolean writesEnabled();
}
