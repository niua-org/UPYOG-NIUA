package org.egov.user.domain.service.asset;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.egov.user.domain.service.utils.UserConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit and integration tests validating in-memory session registry lifecycle, challenge lockout,
 * consent token consumption, and Redis cluster serialization/lockout behaviors in {@link UrbanPropertyOtpSessionRegistry}.
 */
@ExtendWith(MockitoExtension.class)
public class UrbanPropertyOtpSessionRegistryTest {

    private UrbanPropertyOtpSessionRegistry registry;

    @Mock
    private RedisTemplate<String, Object> mockRedisTemplate;

    @Mock
    private ValueOperations<String, Object> mockValueOperations;

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
        registry.registerConsentToken("TOKEN-1", "PT-104", "pb.amritsar", "9876543210", "user-uuid-1", null, 60000L);

        assertTrue(registry.validateConsentToken("PT-104", "TOKEN-1"));
        assertFalse(registry.validateConsentToken("PT-WRONG", "TOKEN-1"));
        assertFalse(registry.validateConsentToken("PT-104", "TOKEN-INVALID"));

        assertTrue(registry.consumeConsentToken("PT-104", "TOKEN-1", null, null));
        assertFalse(registry.validateConsentToken("PT-104", "TOKEN-1"));
    }

    /**
     * Verifies that active verified consent is checked and blank or null tokens are strictly rejected.
     */
    @Test
    @DisplayName("Should validate active verified consent and reject blank tokens")
    void testActiveVerifiedConsentByPropertyId() {
        registry.registerConsentToken("TOKEN-AUTO", "PT-105", "pb.amritsar", "9876543210", null, null, 60000L);

        // validateConsentToken strictly requires valid non-blank consent token
        assertFalse(registry.validateConsentToken("PT-105", null));
        assertFalse(registry.validateConsentToken("PT-105", ""));
        assertTrue(registry.validateConsentToken("PT-105", "TOKEN-AUTO"));
        assertFalse(registry.validateConsentToken("PT-999", "TOKEN-AUTO"));
    }

    /**
     * Tests that when RedisTemplate uses GenericJackson2JsonRedisSerializer without default typing
     * (as in EgovUserApplication#redisTemplate), written OtpChallenge payload is deserialized as LinkedHashMap,
     * and UrbanPropertyOtpSessionRegistry accurately reconstructs the OtpChallenge without failing instanceof check.
     */
    @Test
    @DisplayName("Redis GenericJackson2JsonRedisSerializer: OtpChallenge returns as Map and reconstructs properly")
    void testRedisOtpChallengeSerializationAndResolution() {
        // Build the exact ObjectMapper and Serializer used in EgovUserApplication#redisTemplate
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        Map<String, byte[]> redisStore = new ConcurrentHashMap<>();
        Map<String, Long> redisAttempts = new ConcurrentHashMap<>();

        lenient().when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOperations);

        // Simulate set() via serializer
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            byte[] bytes = serializer.serialize(value);
            redisStore.put(key, bytes);
            return null;
        }).when(mockValueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        // Simulate get() via serializer (which deserializes bytes to LinkedHashMap because there's no @class)
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (key.startsWith(UserConstants.REDIS_PROP_OTP_ATTEMPTS_PREFIX)) {
                return redisAttempts.get(key);
            }
            byte[] bytes = redisStore.get(key);
            return bytes != null ? serializer.deserialize(bytes) : null;
        }).when(mockValueOperations).get(anyString());

        // Simulate increment()
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisAttempts.compute(key, (k, v) -> v == null ? 1L : v + 1L);
        }).when(mockValueOperations).increment(anyString());

        // Simulate delete()
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            boolean removed1 = redisStore.remove(key) != null;
            boolean removed2 = redisAttempts.remove(key) != null;
            return removed1 || removed2;
        }).when(mockRedisTemplate).delete(anyString());

        // Create registry using mock Redis
        UrbanPropertyOtpSessionRegistry redisRegistry = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate, mapper);

        // Register challenge on "Pod 1"
        redisRegistry.registerChallenge("REF-REDIS-1", "PT-200", "pb.amritsar", "9876543210", "caller-1", 60000L, 3);

        // Simulate "Pod 2": has empty local activeChallenges map, must resolve from Redis!
        UrbanPropertyOtpSessionRegistry pod2Registry = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate, mapper);
        UrbanPropertyOtpSessionRegistry.OtpChallenge resolved =
                pod2Registry.resolveActiveChallenge("REF-REDIS-1", "PT-200", "pb.amritsar");

        assertNotNull(resolved, "Challenge should be resolved across pods from Redis");
        assertEquals("PT-200", resolved.getPropertyId());
        assertEquals("pb.amritsar", resolved.getTenantId());
        assertEquals("9876543210", resolved.getMobileNumber());
        assertEquals("caller-1", resolved.getCallerUuid());
        assertFalse(resolved.isExpired());
        assertFalse(resolved.isExhausted());
    }

    /**
     * Tests that failed verification attempts increment the Redis counter via INCR and lockout across pods.
     */
    @Test
    @DisplayName("Redis INCR attempt counter: multiple failed attempts exhaust challenge across pods")
    void testRedisAttemptCounterMultiPodLockout() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        Map<String, byte[]> redisStore = new ConcurrentHashMap<>();
        Map<String, Long> redisAttempts = new ConcurrentHashMap<>();

        lenient().when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOperations);

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            redisStore.put(key, serializer.serialize(value));
            return null;
        }).when(mockValueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (key.startsWith(UserConstants.REDIS_PROP_OTP_ATTEMPTS_PREFIX)) {
                return redisAttempts.get(key);
            }
            byte[] bytes = redisStore.get(key);
            return bytes != null ? serializer.deserialize(bytes) : null;
        }).when(mockValueOperations).get(anyString());

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisAttempts.compute(key, (k, v) -> v == null ? 1L : v + 1L);
        }).when(mockValueOperations).increment(anyString());

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            boolean r1 = redisStore.remove(key) != null;
            boolean r2 = redisAttempts.remove(key) != null;
            return r1 || r2;
        }).when(mockRedisTemplate).delete(anyString());

        // Pod 1 registers challenge with maxAttempts = 3
        UrbanPropertyOtpSessionRegistry pod1 = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate, mapper);
        pod1.registerChallenge("REF-LOCKOUT", "PT-300", "pb.amritsar", "9876543210", 60000L, 3);

        // Pod 2 handles first failed attempt
        UrbanPropertyOtpSessionRegistry pod2 = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate, mapper);
        UrbanPropertyOtpSessionRegistry.OtpChallenge c2 = pod2.resolveActiveChallenge("REF-LOCKOUT", "PT-300", "pb.amritsar");
        assertNotNull(c2);
        assertEquals(1, c2.recordFailedAttempt());
        assertFalse(c2.isExhausted());

        // Pod 3 handles second failed attempt
        UrbanPropertyOtpSessionRegistry pod3 = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate, mapper);
        UrbanPropertyOtpSessionRegistry.OtpChallenge c3 = pod3.resolveActiveChallenge("REF-LOCKOUT", "PT-300", "pb.amritsar");
        assertNotNull(c3);
        assertEquals(2, c3.recordFailedAttempt());
        assertFalse(c3.isExhausted());

        // Pod 1 handles third failed attempt -> reaches maxAttempts (3)
        assertEquals(3, pod1.recordFailedAttempt("REF-LOCKOUT"));

        // Now resolving on ANY pod should see it is exhausted, invalidate it, and return null
        assertNull(pod2.resolveActiveChallenge("REF-LOCKOUT", "PT-300", "pb.amritsar"));
        assertNull(pod1.resolveActiveChallenge("REF-LOCKOUT", "PT-300", "pb.amritsar"));
    }

    /**
     * Tests that verified property consent tokens serialize to Redis, resolve across pods,
     * and atomic deletion on consumption prevents token replay / double-consumption.
     */
    @Test
    @DisplayName("Redis VerifiedPropertyConsent: resolves across pods and single-use consume prevents replay")
    void testRedisConsentTokenMultiPodSingleUse() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        Map<String, byte[]> redisStore = new ConcurrentHashMap<>();

        lenient().when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOperations);

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            redisStore.put(key, serializer.serialize(value));
            return null;
        }).when(mockValueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            byte[] bytes = redisStore.get(key);
            return bytes != null ? serializer.deserialize(bytes) : null;
        }).when(mockValueOperations).get(anyString());

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            byte[] bytes = redisStore.remove(key);
            return bytes != null ? serializer.deserialize(bytes) : null;
        }).when(mockValueOperations).getAndDelete(anyString());

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisStore.remove(key) != null;
        }).when(mockRedisTemplate).delete(anyString());

        // Pod 1 registers consent token
        UrbanPropertyOtpSessionRegistry pod1 = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate, mapper);
        pod1.registerConsentToken("CONSENT-XYZ", "PT-400", "pb.amritsar", "9876543210", "owner-1", "caller-1", 60000L);

        // Pod 2 validates consent token from Redis
        UrbanPropertyOtpSessionRegistry pod2 = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate, mapper);
        assertTrue(pod2.validateConsentToken("PT-400", "CONSENT-XYZ", "caller-1", "pb.amritsar"));

        // Caller mismatch on Pod 2 fails validation
        assertFalse(pod2.validateConsentToken("PT-400", "CONSENT-XYZ", "caller-WRONG", "pb.amritsar"));

        // Pod 2 consumes consent token via atomic getAndDelete
        assertTrue(pod2.consumeConsentToken("PT-400", "CONSENT-XYZ", "caller-1", "pb.amritsar"));

        // Pod 1 attempts to consume again (replay) -> must fail!
        assertFalse(pod1.consumeConsentToken("PT-400", "CONSENT-XYZ", "caller-1", "pb.amritsar"));
        assertFalse(pod2.validateConsentToken("PT-400", "CONSENT-XYZ", "caller-1", "pb.amritsar"));
    }

    /**
     * Tests that concurrent consumption requests (e.g. double-submit or duplicate token in batch)
     * cannot both succeed: atomic removal ensures exactly one request consumes the token.
     */
    @Test
    @DisplayName("Atomic consumption: concurrent calls cannot both consume the same single-use token")
    void testConcurrentConsentTokenConsumption_OnlyOneSucceeds() {
        registry.registerConsentToken("TOKEN-CONCURRENT", "PT-500", "pb.amritsar", "9876543210", "u1", "caller-concurrent", 60000L);

        // Simulate two concurrent requests arriving at the same time
        boolean firstConsume = registry.consumeConsentToken("PT-500", "TOKEN-CONCURRENT", "caller-concurrent", "pb.amritsar");
        boolean secondConsume = registry.consumeConsentToken("PT-500", "TOKEN-CONCURRENT", "caller-concurrent", "pb.amritsar");

        assertTrue(firstConsume, "First caller must successfully consume the token");
        assertFalse(secondConsume, "Second concurrent caller must fail atomic consumption");
    }

    /**
     * Verifies that rate-limiting on (callerUuid, propertyId) prevents an attacker from locking out
     * a genuine owner who uses a different callerUuid and mobile.
     */
    @Test
    @DisplayName("Rate limiting: (callerUuid, propertyId) isolates attacker from real owner")
    void testRateLimiting_PerCallerPropertyIsolation() {
        String propertyId = "PT-RATE-TEST";

        // Attacker makes 3 reservations for owner 1
        assertTrue(registry.tryReserveOtpDispatchSlot("attacker-uuid", propertyId, "9876543210"));
        assertTrue(registry.tryReserveOtpDispatchSlot("attacker-uuid", propertyId, "9876543210"));
        assertTrue(registry.tryReserveOtpDispatchSlot("attacker-uuid", propertyId, "9876543210"));

        // Attacker's 4th reservation is blocked
        assertFalse(registry.tryReserveOtpDispatchSlot("attacker-uuid", propertyId, "9876543210"));

        // Genuine owner (different callerUuid, different mobile) is NOT blocked
        assertTrue(registry.tryReserveOtpDispatchSlot("owner-uuid", propertyId, "9123456789"));
    }

    /**
     * Verifies that rate-limiting on destination mobile prevents SMS flooding to the same phone number.
     */
    @Test
    @DisplayName("Rate limiting: destination mobile throttles after 3 sends to same number")
    void testRateLimiting_DestinationMobileThrottling() {
        String mobile = "9876543210";

        // Even across 3 different callers, mobile receives at most 3 OTPs
        assertTrue(registry.tryReserveOtpDispatchSlot("caller-1", "PT-A", mobile));
        assertTrue(registry.tryReserveOtpDispatchSlot("caller-2", "PT-B", mobile));
        assertTrue(registry.tryReserveOtpDispatchSlot("caller-3", "PT-C", mobile));

        // 4th send to same mobile is rejected to prevent SMS bombing
        assertFalse(registry.tryReserveOtpDispatchSlot("caller-4", "PT-D", mobile));
    }

    /**
     * Verifies that releasing a reserved slot (e.g. after dispatch exception) frees up the reservation.
     */
    @Test
    @DisplayName("Rate limiting: releaseOtpDispatchSlot rolls back reserved slot on dispatch failure")
    void testRateLimiting_ReleaseSlotOnFailure() {
        String callerUuid = "caller-fail";
        String propertyId = "PT-FAIL";
        String mobile = "9876543210";

        assertTrue(registry.tryReserveOtpDispatchSlot(callerUuid, propertyId, mobile));
        assertTrue(registry.tryReserveOtpDispatchSlot(callerUuid, propertyId, mobile));
        assertTrue(registry.tryReserveOtpDispatchSlot(callerUuid, propertyId, mobile));
        assertFalse(registry.tryReserveOtpDispatchSlot(callerUuid, propertyId, mobile));

        // Downstream dispatch failed, release slot
        registry.releaseOtpDispatchSlot(callerUuid, propertyId, mobile);

        // Can now successfully reserve slot again
        assertTrue(registry.tryReserveOtpDispatchSlot(callerUuid, propertyId, mobile));
    }

    /**
     * Verifies that Redis distributed atomic rate limiting shares counters across pods via INCR.
     */
    @Test
    @DisplayName("Redis distributed rate limiting: INCR across pods enforces global limit")
    void testRateLimiting_RedisDistributedAtomicReservation() {
        Map<String, Long> redisCounters = new ConcurrentHashMap<>();

        lenient().when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOperations);

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisCounters.compute(key, (k, v) -> v == null ? 1L : v + 1L);
        }).when(mockValueOperations).increment(anyString());

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisCounters.compute(key, (k, v) -> v == null ? 0L : v - 1L);
        }).when(mockValueOperations).decrement(anyString());

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisCounters.get(key);
        }).when(mockValueOperations).get(anyString());

        // Pod 1 makes 2 reservations
        UrbanPropertyOtpSessionRegistry pod1 = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate);
        assertTrue(pod1.tryReserveOtpDispatchSlot("user-1", "PT-REDIS-RATE", "9876543210"));
        assertTrue(pod1.tryReserveOtpDispatchSlot("user-1", "PT-REDIS-RATE", "9876543210"));

        // Pod 2 makes 3rd reservation (hits limit of 3)
        UrbanPropertyOtpSessionRegistry pod2 = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate);
        assertTrue(pod2.tryReserveOtpDispatchSlot("user-1", "PT-REDIS-RATE", "9876543210"));

        // Pod 3 attempts 4th reservation across cluster -> blocked!
        UrbanPropertyOtpSessionRegistry pod3 = new UrbanPropertyOtpSessionRegistry(mockRedisTemplate);
        assertFalse(pod3.tryReserveOtpDispatchSlot("user-1", "PT-REDIS-RATE", "9876543210"));
    }
}
