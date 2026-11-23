package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
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
     * Verifies that the consent validation endpoint returns valid=true for an active, unexpired consent token.
     */
    @Test
    @DisplayName("Validate consent token endpoint should return valid=true for existing token")
    void testValidateConsentToken_Endpoint() {
        sessionRegistry.registerConsentToken("TOKEN-ABC", PROPERTY_ID, TENANT_ID, "9876543210", null, 60000L);

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
        sessionRegistry.registerConsentToken("TOKEN-ABC", PROPERTY_ID, TENANT_ID, "9876543210", null, 60000L);

        UrbanPropertyConsentValidateRequest request = UrbanPropertyConsentValidateRequest.builder()
                .propertyId(PROPERTY_ID)
                .consentToken(null)
                .build();

        UrbanPropertyConsentValidateResponse response = service.validateConsentToken(request);

        assertNotNull(response);
        assertFalse(response.getValid());
    }
}
