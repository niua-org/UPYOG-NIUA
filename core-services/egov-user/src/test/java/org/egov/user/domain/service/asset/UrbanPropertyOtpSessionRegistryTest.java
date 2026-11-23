package org.egov.user.domain.service.asset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests validating in-memory session registry lifecycle, challenge lockout, and consent token consumption
 * in {@link UrbanPropertyOtpSessionRegistry}.
 */
public class UrbanPropertyOtpSessionRegistryTest {

    private UrbanPropertyOtpSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new UrbanPropertyOtpSessionRegistry();
    }

    /**
     * Verifies that registering an OTP challenge correctly associates the reference with property, tenant, and mobile.
     */
    @Test
    @DisplayName("Should register challenge and resolve it accurately")
    void testRegisterAndResolveChallenge() {
        registry.registerChallenge("REF-1", "PT-101", "pb.amritsar", "9876543210", 60000L, 3);
        UrbanPropertyOtpSessionRegistry.OtpChallenge challenge =
                registry.resolveActiveChallenge("REF-1", "PT-101", "pb.amritsar");

        assertNotNull(challenge);
        assertEquals("PT-101", challenge.getPropertyId());
        assertEquals("9876543210", challenge.getMobileNumber());
        assertFalse(challenge.isExpired());
        assertFalse(challenge.isExhausted());
    }

    /**
     * Verifies that failed attempts are recorded and the challenge is locked out once maximum attempts are reached.
     */
    @Test
    @DisplayName("Should record failed attempts and exhaust challenge")
    void testChallengeExhaustion() {
        registry.registerChallenge("REF-2", "PT-102", "pb.amritsar", "9876543210", 60000L, 2);
        UrbanPropertyOtpSessionRegistry.OtpChallenge challenge =
                registry.resolveActiveChallenge("REF-2", "PT-102", "pb.amritsar");

        assertNotNull(challenge);
        assertEquals(1, challenge.recordFailedAttempt());
        assertFalse(challenge.isExhausted());

        assertEquals(2, challenge.recordFailedAttempt());
        assertTrue(challenge.isExhausted());

        // Resolving again should return null because exhausted
        assertNull(registry.resolveActiveChallenge("REF-2", "PT-102", "pb.amritsar"));
    }

    /**
     * Verifies that resolving a challenge with mismatched property ID or tenant returns null.
     */
    @Test
    @DisplayName("Should return null for mismatched property or tenant")
    void testMismatchedChallenge() {
        registry.registerChallenge("REF-3", "PT-103", "pb.amritsar", "9876543210", 60000L, 3);
        assertNull(registry.resolveActiveChallenge("REF-3", "PT-WRONG", "pb.amritsar"));
        assertNull(registry.resolveActiveChallenge("REF-3", "PT-103", "pb.ludhiana"));
    }

    /**
     * Verifies that consent tokens are registered, validated, and once consumed, cannot be reused.
     */
    @Test
    @DisplayName("Should register, validate, and consume verified consent token")
    void testConsentTokenLifecycle() {
        registry.registerConsentToken("TOKEN-1", "PT-104", "pb.amritsar", "9876543210", "user-uuid-1", 60000L);

        assertTrue(registry.validateConsentToken("PT-104", "TOKEN-1"));
        assertFalse(registry.validateConsentToken("PT-WRONG", "TOKEN-1"));
        assertFalse(registry.validateConsentToken("PT-104", "TOKEN-INVALID"));

        assertTrue(registry.consumeConsentToken("PT-104", "TOKEN-1"));
        assertFalse(registry.validateConsentToken("PT-104", "TOKEN-1"));
    }

    /**
     * Verifies that active verified consent is checked and blank or null tokens are strictly rejected.
     */
    @Test
    @DisplayName("Should validate active verified consent by propertyId and reject blank tokens")
    void testActiveVerifiedConsentByPropertyId() {
        registry.registerConsentToken("TOKEN-AUTO", "PT-105", "pb.amritsar", "9876543210", null, 60000L);

        assertTrue(registry.hasActiveVerifiedConsentForProperty("PT-105"));
        assertFalse(registry.hasActiveVerifiedConsentForProperty("PT-999"));

        // validateConsentToken strictly requires valid non-blank consent token
        assertFalse(registry.validateConsentToken("PT-105", null));
        assertFalse(registry.validateConsentToken("PT-105", ""));
        assertTrue(registry.validateConsentToken("PT-105", "TOKEN-AUTO"));
        assertFalse(registry.validateConsentToken("PT-999", "TOKEN-AUTO"));
    }
}
