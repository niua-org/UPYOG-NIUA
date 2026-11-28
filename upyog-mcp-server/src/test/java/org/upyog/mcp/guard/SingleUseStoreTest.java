package org.upyog.mcp.guard;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleUseStoreTest {

    @Test
    void tokenCanBeConsumedOnlyOnce() {
        SingleUseStore store = new MemoryStore();
        assertTrue(store.consumeOnce("jti-1", Duration.ofMinutes(5)));
        assertFalse(store.consumeOnce("jti-1", Duration.ofMinutes(5)));
    }

    @Test
    void rejectingStoreDoesNotPretendToBeDistributed() {
        RejectingSingleUseStore store = new RejectingSingleUseStore();
        assertFalse(store.writesEnabled());
    }

    private static final class MemoryStore implements SingleUseStore {
        private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();

        @Override
        public boolean consumeOnce(String key, Duration ttl) {
            return values.putIfAbsent(key, "1") == null;
        }

        @Override
        public void saveResult(String key, String json, Duration ttl) {
            values.put(key, json);
        }

        @Override
        public Optional<String> findResult(String key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public boolean writesEnabled() {
            return true;
        }
    }
}
