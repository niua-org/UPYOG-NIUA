package org.upyog.family.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamilyOtpSessionRegistryTest {

    private FamilyOtpSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new FamilyOtpSessionRegistry();
    }

    @Test
    @DisplayName("resolveActiveChallenge returns challenge for matching reference and mobile")
    void resolveActiveChallenge_MatchingContext_ReturnsChallenge() {
        registry.register("REF-1", "9876543210", "pg.citya", 60_000L, 3);

        FamilyOtpSessionRegistry.OtpChallenge challenge =
                registry.resolveActiveChallenge("REF-1", "9876543210", "pg.citya");

        assertNotNull(challenge);
        assertFalse(challenge.isExhausted());
    }

    @Test
    @DisplayName("failed attempts invalidate session when max attempts reached")
    void recordFailedAttempts_ExhaustsSession() {
        registry.register("REF-2", "9876543210", "pg.citya", 60_000L, 2);

        FamilyOtpSessionRegistry.OtpChallenge challenge =
                registry.resolveActiveChallenge("REF-2", "9876543210", "pg.citya");
        assertNotNull(challenge);

        challenge.recordFailedAttempt();
        challenge.recordFailedAttempt();

        assertTrue(challenge.isExhausted());
        registry.invalidate("REF-2");
        assertNull(registry.resolveActiveChallenge("REF-2", "9876543210", "pg.citya"));
    }

    @Test
    @DisplayName("consume removes challenge so it cannot be reused")
    void consume_RemovesChallenge() {
        registry.register("REF-3", "9876543210", "pg.citya", 60_000L, 3);
        FamilyOtpSessionRegistry.OtpChallenge challenge =
                registry.resolveActiveChallenge("REF-3", "9876543210", "pg.citya");
        registry.consume(challenge);
        assertNull(registry.resolveActiveChallenge("REF-3", "9876543210", "pg.citya"));
    }
}
