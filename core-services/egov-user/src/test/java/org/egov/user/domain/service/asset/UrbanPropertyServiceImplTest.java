package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.PropertyOtpClient;
import org.egov.user.persistence.repository.PropertyServiceClient;
import org.egov.user.persistence.repository.UserPropertyRepository;
import org.egov.user.web.contract.property.UrbanPropertyConsentValidateRequest;
import org.egov.user.web.contract.property.UrbanPropertyConsentValidateResponse;
import org.egov.user.web.contract.property.UrbanPropertyInfo;
import org.egov.user.web.contract.property.UrbanPropertyOtpRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpResponse;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyResponse;
import org.egov.user.web.contract.property.UrbanPropertyOwnerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests validating ownership business logic, OTP dispatch, verification, and consent handling in {@link UrbanPropertyServiceImpl}.
 */
public class UrbanPropertyServiceImplTest {

    private PropertyServiceClient propertyServiceClient;
    private UserPropertyRepository userPropertyRepository;
    private UrbanPropertyOtpSessionRegistry sessionRegistry;
    private UrbanPropertyOwnershipValidator ownershipValidator;
    private PropertyOtpClient propertyOtpClient;
    private UserConfiguration userConfiguration;
    private UrbanPropertyServiceImpl service;

    private static final String TENANT_ID = "pb.amritsar";
    private static final String PROPERTY_ID = "PB-PT-2026-001";

    @BeforeEach
    void setUp() {
        propertyServiceClient = mock(PropertyServiceClient.class);
        userPropertyRepository = mock(UserPropertyRepository.class);
        sessionRegistry = new UrbanPropertyOtpSessionRegistry();
        ownershipValidator = new UrbanPropertyOwnershipValidator();
        propertyOtpClient = mock(PropertyOtpClient.class);
        userConfiguration = mock(UserConfiguration.class);

        when(userConfiguration.getPropertyOtpFixedEnabled()).thenReturn(true);
        when(userConfiguration.getPropertyOtpFixedValue()).thenReturn("123456");
        when(userConfiguration.getPropertyOtpReferenceTtlMs()).thenReturn(900000L);
        when(userConfiguration.getPropertyOtpVerifyMaxAttempts()).thenReturn(3);
        when(userConfiguration.getPropertyConsentTokenTtlMs()).thenReturn(900000L);
        when(userConfiguration.getPropertyServiceSystemRole()).thenReturn("INTERNAL_MICROSERVICE_ROLE");

        when(propertyOtpClient.validateOtp(any(), eq("123456"), any(), any())).thenReturn(true);
        when(propertyOtpClient.validateOtp(any(), eq("999999"), any(), any())).thenReturn(false);

        service = new UrbanPropertyServiceImpl(
                propertyServiceClient,
                userPropertyRepository,
                sessionRegistry,
                ownershipValidator,
                propertyOtpClient,
                userConfiguration
        );
    }

    /**
     * Verifies that sending OTP for a single-owner property already linked to a citizen profile fails with PROPERTY_ALREADY_LINKED.
     */
    @Test
    @DisplayName("Send OTP for Individual property when already linked should throw PROPERTY_ALREADY_LINKED")
    void testSendOtp_Individual_AlreadyLinked_ThrowsError() {
        UrbanPropertyOwnerInfo owner = UrbanPropertyOwnerInfo.builder()
                .name("Ramesh Kumar")
                .rawMobileNumber("9876543210")
                .build();
        UrbanPropertyInfo info = UrbanPropertyInfo.builder()
                .propertyId(PROPERTY_ID)
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.SINGLEOWNER")
                .owners(Collections.singletonList(owner))
                .build();

        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq(PROPERTY_ID), any()))
                .thenReturn(info);

        Map<String, Object> linkedRow = new HashMap<>();
        linkedRow.put("userUuid", "citizen-existing-1");
        linkedRow.put("mobileNumber", "9876543210");
        when(userPropertyRepository.findActiveLinkedUsersForUrbanProperty(eq(TENANT_ID), eq(PROPERTY_ID)))
                .thenReturn(Collections.singletonList(linkedRow));

        UrbanPropertyOtpRequest request = UrbanPropertyOtpRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId(PROPERTY_ID)
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> service.sendUrbanPropertyOtp(request));
        assertEquals("PROPERTY_ALREADY_LINKED", ex.getCode());
    }

    /**
     * Verifies that sending OTP for an unlinked single-owner property succeeds and generates an OTP reference.
     */
    @Test
    @DisplayName("Send OTP for Individual property unlinked should succeed and send OTP")
    void testSendOtp_Individual_Unlinked_Success() {
        UrbanPropertyOwnerInfo owner = UrbanPropertyOwnerInfo.builder()
                .name("Ramesh Kumar")
                .rawMobileNumber("9876543210")
                .build();
        UrbanPropertyInfo info = UrbanPropertyInfo.builder()
                .propertyId(PROPERTY_ID)
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.SINGLEOWNER")
                .owners(Collections.singletonList(owner))
                .build();

        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq(PROPERTY_ID), any()))
                .thenReturn(info);
        when(userPropertyRepository.findActiveLinkedUsersForUrbanProperty(eq(TENANT_ID), eq(PROPERTY_ID)))
                .thenReturn(Collections.emptyList());

        UrbanPropertyOtpRequest request = UrbanPropertyOtpRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId(PROPERTY_ID)
                .build();

        UrbanPropertyOtpResponse response = service.sendUrbanPropertyOtp(request);

        assertNotNull(response);
        assertNotNull(response.getReferenceId());
        assertEquals(PROPERTY_ID, response.getPropertyId());
    }

    /**
     * Verifies that sending OTP for a multi-owner property when all slots are occupied throws PROPERTY_MAX_OWNERS_EXCEEDED.
     */
    @Test
    @DisplayName("Send OTP for Multi-owner property when all owners already linked should throw PROPERTY_MAX_OWNERS_EXCEEDED")
    void testSendOtp_MultiOwner_AllLinked_ThrowsError() {
        List<UrbanPropertyOwnerInfo> owners = new ArrayList<>();
        owners.add(UrbanPropertyOwnerInfo.builder().name("Owner 1").rawMobileNumber("9876543210").build());
        owners.add(UrbanPropertyOwnerInfo.builder().name("Owner 2").rawMobileNumber("9123456789").build());

        UrbanPropertyInfo info = UrbanPropertyInfo.builder()
                .propertyId(PROPERTY_ID)
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.MULTIPLEOWNERS")
                .owners(owners)
                .build();

        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq(PROPERTY_ID), any()))
                .thenReturn(info);

        List<Map<String, Object>> linked = new ArrayList<>();
        Map<String, Object> l1 = new HashMap<>();
        l1.put("userUuid", "u1");
        l1.put("mobileNumber", "9876543210");
        Map<String, Object> l2 = new HashMap<>();
        l2.put("userUuid", "u2");
        l2.put("mobileNumber", "9123456789");
        linked.add(l1);
        linked.add(l2);

        when(userPropertyRepository.findActiveLinkedUsersForUrbanProperty(eq(TENANT_ID), eq(PROPERTY_ID)))
                .thenReturn(linked);

        UrbanPropertyOtpRequest request = UrbanPropertyOtpRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId(PROPERTY_ID)
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> service.sendUrbanPropertyOtp(request));
        assertEquals("PROPERTY_MAX_OWNERS_EXCEEDED", ex.getCode());
    }

    /**
     * Verifies that sending OTP when the calling citizen is already linked to the property throws PROPERTY_ALREADY_LINKED_TO_CALLER.
     */
    @Test
    @DisplayName("Send OTP for Multi-owner property when current caller is already linked should throw PROPERTY_ALREADY_LINKED_TO_CALLER")
    void testSendOtp_MultiOwner_CallerAlreadyLinked_ThrowsError() {
        List<UrbanPropertyOwnerInfo> owners = new ArrayList<>();
        owners.add(UrbanPropertyOwnerInfo.builder().name("Owner 1").rawMobileNumber("9876543210").build());
        owners.add(UrbanPropertyOwnerInfo.builder().name("Owner 2").rawMobileNumber("9123456789").build());

        UrbanPropertyInfo info = UrbanPropertyInfo.builder()
                .propertyId(PROPERTY_ID)
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.MULTIPLEOWNERS")
                .owners(owners)
                .build();

        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq(PROPERTY_ID), any()))
                .thenReturn(info);

        List<Map<String, Object>> linked = new ArrayList<>();
        Map<String, Object> l1 = new HashMap<>();
        l1.put("userUuid", "caller-uuid-1");
        l1.put("mobileNumber", "9876543210");
        linked.add(l1);

        when(userPropertyRepository.findActiveLinkedUsersForUrbanProperty(eq(TENANT_ID), eq(PROPERTY_ID)))
                .thenReturn(linked);

        RequestInfo reqInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("caller-uuid-1").build())
                .build();

        UrbanPropertyOtpRequest request = UrbanPropertyOtpRequest.builder()
                .requestInfo(reqInfo)
                .tenantId(TENANT_ID)
                .propertyId(PROPERTY_ID)
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> service.sendUrbanPropertyOtp(request));
        assertEquals("PROPERTY_ALREADY_LINKED_TO_CALLER", ex.getCode());
    }

    /**
     * Verifies that sending OTP for a multi-owner property with exactly one unlinked owner automatically selects that owner.
     */
    @Test
    @DisplayName("Send OTP for Multi-owner property with 1 unlinked owner should auto-select unlinked owner")
    void testSendOtp_MultiOwner_OneUnlinkedRemaining_AutoSelected() {
        List<UrbanPropertyOwnerInfo> owners = new ArrayList<>();
        owners.add(UrbanPropertyOwnerInfo.builder().name("Owner 1").rawMobileNumber("9876543210").build());
        owners.add(UrbanPropertyOwnerInfo.builder().name("Owner 2").rawMobileNumber("9123456789").build());

        UrbanPropertyInfo info = UrbanPropertyInfo.builder()
                .propertyId(PROPERTY_ID)
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.MULTIPLEOWNERS")
                .owners(owners)
                .build();

        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq(PROPERTY_ID), any()))
                .thenReturn(info);

        List<Map<String, Object>> linked = new ArrayList<>();
        Map<String, Object> l1 = new HashMap<>();
        l1.put("userUuid", "other-user-uuid");
        l1.put("mobileNumber", "9876543210");
        linked.add(l1);

        when(userPropertyRepository.findActiveLinkedUsersForUrbanProperty(eq(TENANT_ID), eq(PROPERTY_ID)))
                .thenReturn(linked);

        UrbanPropertyOtpRequest request = UrbanPropertyOtpRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId(PROPERTY_ID)
                .build();

        UrbanPropertyOtpResponse response = service.sendUrbanPropertyOtp(request);

        assertNotNull(response);
        assertNotNull(response.getReferenceId());
        assertEquals("91******89", response.getMaskedMobileNumber());
    }

    /**
     * Verifies that submitting a valid OTP generates and returns a cryptographically random propertyConsentToken.
     */
    @Test
    @DisplayName("Verify OTP with valid fixed OTP should issue consent token")
    void testVerifyOtp_Valid_IssuesConsentToken() {
        sessionRegistry.registerChallenge("REF-123", PROPERTY_ID, TENANT_ID, "9876543210", 60000L, 3);

        UrbanPropertyOtpVerifyRequest request = UrbanPropertyOtpVerifyRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId(PROPERTY_ID)
                .referenceId("REF-123")
                .otp("123456")
                .build();

        UrbanPropertyOtpVerifyResponse response = service.verifyUrbanPropertyOtp(request);

        assertNotNull(response);
        assertTrue(response.getVerified());
        assertNotNull(response.getPropertyConsentToken());
        assertTrue(response.getPropertyConsentToken().startsWith("PROP-CONSENT-"));

        // Validate the issued token programmatically
        assertTrue(service.validatePropertyConsentToken(PROPERTY_ID, response.getPropertyConsentToken()));
    }

    /**
     * Verifies that submitting an incorrect OTP fails with INVALID_OTP.
     */
    @Test
    @DisplayName("Verify OTP with invalid OTP should throw INVALID_OTP")
    void testVerifyOtp_InvalidOtp_ThrowsError() {
        sessionRegistry.registerChallenge("REF-123", PROPERTY_ID, TENANT_ID, "9876543210", 60000L, 3);

        UrbanPropertyOtpVerifyRequest request = UrbanPropertyOtpVerifyRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId(PROPERTY_ID)
                .referenceId("REF-123")
                .otp("999999")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> service.verifyUrbanPropertyOtp(request));
        assertEquals("INVALID_OTP", ex.getCode());
    }

    /**
     * Verifies that exhausting maximum OTP verification attempts throws MAX_OTP_ATTEMPTS_EXCEEDED
     * and invalidates the session registry challenge.
     */
    @Test
    @DisplayName("Verify OTP exhausting max attempts should throw MAX_OTP_ATTEMPTS_EXCEEDED")
    void testVerifyOtp_MaxAttemptsExceeded_ThrowsError() {
        sessionRegistry.registerChallenge("REF-MAX", PROPERTY_ID, TENANT_ID, "9876543210", 60000L, 3);

        UrbanPropertyOtpVerifyRequest request = UrbanPropertyOtpVerifyRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId(PROPERTY_ID)
                .referenceId("REF-MAX")
                .otp("999999")
                .build();

        // Attempt 1: INVALID_OTP
        CustomException ex1 = assertThrows(CustomException.class, () -> service.verifyUrbanPropertyOtp(request));
        assertEquals("INVALID_OTP", ex1.getCode());

        // Attempt 2: INVALID_OTP
        CustomException ex2 = assertThrows(CustomException.class, () -> service.verifyUrbanPropertyOtp(request));
        assertEquals("INVALID_OTP", ex2.getCode());

        // Attempt 3: MAX_OTP_ATTEMPTS_EXCEEDED
        CustomException ex3 = assertThrows(CustomException.class, () -> service.verifyUrbanPropertyOtp(request));
        assertEquals("MAX_OTP_ATTEMPTS_EXCEEDED", ex3.getCode());

        // Subsequent attempt: challenge is now invalidated / expired
        CustomException ex4 = assertThrows(CustomException.class, () -> service.verifyUrbanPropertyOtp(request));
        assertEquals("INVALID_OTP_SESSION", ex4.getCode());
    }

    /**
     * Verifies that the consent validation endpoint returns valid=true for an active, unexpired consent token.
     */
    @Test
    @DisplayName("Validate consent token endpoint should return valid=true for existing token")
    void testValidateConsentToken_Endpoint() {
        sessionRegistry.registerConsentToken("TOKEN-ABC", PROPERTY_ID, TENANT_ID, "9876543210", null, null, 60000L);

        UrbanPropertyConsentValidateRequest request = UrbanPropertyConsentValidateRequest.builder()
                .propertyId(PROPERTY_ID)
                .consentToken("TOKEN-ABC")
                .build();

        UrbanPropertyConsentValidateResponse response = service.validateConsentToken(request);

        assertNotNull(response);
        assertTrue(response.getValid());
    }

    /**
     * Verifies that the consent validation endpoint strictly rejects null or blank consent tokens.
     */
    @Test
    @DisplayName("Validate consent token endpoint should return invalid when consentToken is null")
    void testValidateConsentToken_NullToken_Rejected() {
        sessionRegistry.registerConsentToken("TOKEN-ABC", PROPERTY_ID, TENANT_ID, "9876543210", null, null, 60000L);

        UrbanPropertyConsentValidateRequest request = UrbanPropertyConsentValidateRequest.builder()
                .propertyId(PROPERTY_ID)
                .consentToken(null)
                .build();

        UrbanPropertyConsentValidateResponse response = service.validateConsentToken(request);

        assertNotNull(response);
        assertFalse(response.getValid());
    }

    @Test
    @DisplayName("consumeConsentToken validates, consumes single-use token, and prevents replay")
    void testConsumeConsentToken_SingleUse() {
        UrbanPropertyInfo property = UrbanPropertyInfo.builder()
                .propertyId(PROPERTY_ID)
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.SINGLEOWNER")
                .owners(Collections.singletonList(UrbanPropertyOwnerInfo.builder().rawMobileNumber("9876543210").build()))
                .build();
        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq(PROPERTY_ID), any()))
                .thenReturn(property);

        sessionRegistry.registerConsentToken("TOKEN-ONCE", PROPERTY_ID, TENANT_ID, "9876543210", null, "caller-uuid-1", 60000L);

        // First consumption succeeds
        boolean first = service.consumeConsentToken(PROPERTY_ID, "TOKEN-ONCE", "caller-uuid-1", TENANT_ID);
        assertTrue(first);

        // Second consumption fails (single-use replay prevention)
        boolean second = service.consumeConsentToken(PROPERTY_ID, "TOKEN-ONCE", "caller-uuid-1", TENANT_ID);
        assertFalse(second);
    }

    @Test
    @DisplayName("consumeConsentToken fails closed when non-CustomException occurs during capacity re-check")
    void testConsumeConsentToken_NonCustomException_FailsClosedWithCustomException() {
        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq(PROPERTY_ID), any()))
                .thenThrow(new RuntimeException("property-services connection reset"));

        sessionRegistry.registerConsentToken("TOKEN-ERR", PROPERTY_ID, TENANT_ID, "9876543210", null, "caller-uuid-1", 60000L);

        CustomException ex = assertThrows(CustomException.class, () ->
                service.consumeConsentToken(PROPERTY_ID, "TOKEN-ERR", "caller-uuid-1", TENANT_ID));
        assertEquals(UserConstants.ERROR_PROPERTY_SERVICE_ERROR, ex.getCode());
    }

    @Test
    @DisplayName("consumeConsentToken throws PROPERTY_NOT_FOUND when property service returns null")
    void testConsumeConsentToken_PropertyNotFound_ThrowsCustomException() {
        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq(PROPERTY_ID), any()))
                .thenReturn(null);

        sessionRegistry.registerConsentToken("TOKEN-404", PROPERTY_ID, TENANT_ID, "9876543210", null, "caller-uuid-1", 60000L);

        CustomException ex = assertThrows(CustomException.class, () ->
                service.consumeConsentToken(PROPERTY_ID, "TOKEN-404", "caller-uuid-1", TENANT_ID));
        assertEquals(UserConstants.ERROR_PROPERTY_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("consumeConsentToken throws PROPERTY_LINKING_LIMIT_EXCEEDED when capacity is exceeded")
    void testConsumeConsentToken_LinkingLimitExceeded_ThrowsCustomException() {
        UrbanPropertyInfo property = UrbanPropertyInfo.builder()
                .propertyId(PROPERTY_ID)
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.SINGLEOWNER")
                .owners(Collections.singletonList(UrbanPropertyOwnerInfo.builder().rawMobileNumber("9876543210").build()))
                .build();
        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq(PROPERTY_ID), any()))
                .thenReturn(property);

        Map<String, Object> linkedRow = new HashMap<>();
        linkedRow.put("userUuid", "already-linked-user");
        when(userPropertyRepository.findActiveLinkedUsersForUrbanProperty(eq(TENANT_ID), eq(PROPERTY_ID)))
                .thenReturn(Collections.singletonList(linkedRow));

        sessionRegistry.registerConsentToken("TOKEN-FULL", PROPERTY_ID, TENANT_ID, "9876543210", null, "caller-uuid-1", 60000L);

        CustomException ex = assertThrows(CustomException.class, () ->
                service.consumeConsentToken(PROPERTY_ID, "TOKEN-FULL", "caller-uuid-1", TENANT_ID));
        assertEquals(UserConstants.ERROR_PROPERTY_LINKING_LIMIT_EXCEEDED, ex.getCode());
    }

    @Test
    @DisplayName("consumeConsentToken fails when caller UUID does not match token binding")
    void testConsumeConsentToken_CallerMismatch() {
        sessionRegistry.registerConsentToken("TOKEN-BOUND", PROPERTY_ID, TENANT_ID, "9876543210", null, "legit-caller", 60000L);

        boolean result = service.consumeConsentToken(PROPERTY_ID, "TOKEN-BOUND", "attacker-caller", TENANT_ID);
        assertFalse(result);
    }

    @Test
    @DisplayName("sendUrbanPropertyOtp throws OTP_RATE_LIMIT_EXCEEDED when dispatch count exceeds 3 in window")
    void testSendUrbanPropertyOtp_RateLimitExceeded() {
        UrbanPropertyOwnerInfo owner = UrbanPropertyOwnerInfo.builder()
                .name("Ramesh Kumar")
                .rawMobileNumber("9876543210")
                .build();
        UrbanPropertyInfo info = UrbanPropertyInfo.builder()
                .propertyId("PB-PT-RATE")
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.SINGLEOWNER")
                .owners(Collections.singletonList(owner))
                .build();

        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq("PB-PT-RATE"), any()))
                .thenReturn(info);
        when(userPropertyRepository.findActiveLinkedUsersForUrbanProperty(eq(TENANT_ID), eq("PB-PT-RATE")))
                .thenReturn(Collections.emptyList());

        UrbanPropertyOtpRequest req = UrbanPropertyOtpRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId("PB-PT-RATE")
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("spammer-uuid").build()).build())
                .build();

        // 3 allowed sends
        service.sendUrbanPropertyOtp(req);
        service.sendUrbanPropertyOtp(req);
        service.sendUrbanPropertyOtp(req);

        // 4th send blocked by rate limiter
        CustomException ex = assertThrows(CustomException.class, () -> service.sendUrbanPropertyOtp(req));
        assertEquals("OTP_RATE_LIMIT_EXCEEDED", ex.getCode());
    }

    /**
     * Verifies that when an attacker caller exhausts their rate limit on a property,
     * genuine owners with different caller UUID and destination mobile are NOT locked out.
     */
    @Test
    @DisplayName("Rate limiting on (callerUuid, propertyId) prevents attacker from locking out real owner")
    void testSendUrbanPropertyOtp_AttackerRateLimited_RealOwnerNotBlocked() {
        List<UrbanPropertyOwnerInfo> owners = new ArrayList<>();
        owners.add(UrbanPropertyOwnerInfo.builder().name("Owner 1").rawMobileNumber("9876543210").build());
        owners.add(UrbanPropertyOwnerInfo.builder().name("Owner 2").rawMobileNumber("9123456789").build());

        UrbanPropertyInfo info = UrbanPropertyInfo.builder()
                .propertyId("PB-PT-SHARED")
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.MULTIPLEOWNERS")
                .owners(owners)
                .build();

        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq("PB-PT-SHARED"), any()))
                .thenReturn(info);
        when(userPropertyRepository.findActiveLinkedUsersForUrbanProperty(eq(TENANT_ID), eq("PB-PT-SHARED")))
                .thenReturn(Collections.emptyList());

        // Attacker caller tries 3 times to send OTP for Owner 1
        UrbanPropertyOtpRequest attackerReq = UrbanPropertyOtpRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId("PB-PT-SHARED")
                .ownerMobileNumber("9876543210")
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("attacker-uuid").build()).build())
                .build();

        service.sendUrbanPropertyOtp(attackerReq);
        service.sendUrbanPropertyOtp(attackerReq);
        service.sendUrbanPropertyOtp(attackerReq);

        // 4th attempt by attacker is rate-limited
        CustomException ex = assertThrows(CustomException.class, () -> service.sendUrbanPropertyOtp(attackerReq));
        assertEquals("OTP_RATE_LIMIT_EXCEEDED", ex.getCode());

        // Genuine Owner 2 with different callerUuid and mobile can still request OTP
        UrbanPropertyOtpRequest ownerReq = UrbanPropertyOtpRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId("PB-PT-SHARED")
                .ownerMobileNumber("9123456789")
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("owner2-uuid").build()).build())
                .build();

        UrbanPropertyOtpResponse ownerResp = service.sendUrbanPropertyOtp(ownerReq);
        assertNotNull(ownerResp);
        assertEquals("PB-PT-SHARED", ownerResp.getPropertyId());
    }

    @Test
    @DisplayName("sendUrbanPropertyOtp should use configured system role in system RequestInfo when calling property-services")
    void testSendUrbanPropertyOtp_UsesConfiguredSystemRoleInRequestInfo() {
        UrbanPropertyOwnerInfo owner = UrbanPropertyOwnerInfo.builder()
                .name("Ramesh Kumar")
                .rawMobileNumber("9876543210")
                .build();
        UrbanPropertyInfo info = UrbanPropertyInfo.builder()
                .propertyId("PB-PT-ROLE-CHECK")
                .tenantId(TENANT_ID)
                .ownershipCategory("INDIVIDUAL.SINGLEOWNER")
                .owners(Collections.singletonList(owner))
                .build();

        org.mockito.ArgumentCaptor<RequestInfo> requestInfoCaptor = org.mockito.ArgumentCaptor.forClass(RequestInfo.class);
        when(propertyServiceClient.fetchPropertyById(eq(TENANT_ID), eq("PB-PT-ROLE-CHECK"), requestInfoCaptor.capture()))
                .thenReturn(info);
        when(userPropertyRepository.findActiveLinkedUsersForUrbanProperty(eq(TENANT_ID), eq("PB-PT-ROLE-CHECK")))
                .thenReturn(Collections.emptyList());

        UrbanPropertyOtpRequest request = UrbanPropertyOtpRequest.builder()
                .tenantId(TENANT_ID)
                .propertyId("PB-PT-ROLE-CHECK")
                .build();

        service.sendUrbanPropertyOtp(request);

        RequestInfo captured = requestInfoCaptor.getValue();
        assertNotNull(captured);
        assertNotNull(captured.getUserInfo());
        assertEquals("SYSTEM", captured.getUserInfo().getType());
        assertEquals(1, captured.getUserInfo().getRoles().size());
        assertEquals("INTERNAL_MICROSERVICE_ROLE", captured.getUserInfo().getRoles().get(0).getCode());
    }
}
