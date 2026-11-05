package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.IdGenRepository;
import org.upyog.family.persistence.repository.OtpClient;
import org.upyog.family.persistence.repository.RequesterDxClient;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyOtpRequest;
import org.upyog.family.web.contract.FamilyOtpResponse;
import org.upyog.family.web.contract.FamilyOtpVerifyRequest;
import org.upyog.family.web.contract.FamilyOtpVerifyResponse;
import org.upyog.family.web.contract.FamilySearchCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyOtpFlowTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private OtpClient otpClient;

    @Mock
    private RequesterDxClient requesterDxClient;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private FamilyOtpServiceImpl familyOtpService;

    private Family activeFamily;
    private RequestInfo requestInfo;
    private CitizenProfile headProfile;

    @BeforeEach
    void setUp() {
        requestInfo = RequestInfo.builder()
                .apiId("org.upyog.family")
                .ver("1.0.0")
                .ts(1729500050000L)
                .userInfo(User.builder().uuid("applicant-uuid").build())
                .build();

        FamilyMember head = FamilyMember.builder()
                .userUuid("head-uuid-1")
                .relationshipWithHead("HEAD")
                .isHead(true)
                .active(true)
                .build();

        activeFamily = Family.builder()
                .id("fam-internal-id")
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .headUserUuid("head-uuid-1")
                .status(FamilyStatus.ACTIVE)
                .members(new ArrayList<>(Collections.singletonList(head)))
                .build();

        headProfile = CitizenProfile.builder()
                .uuid("head-uuid-1")
                .name("Ramesh Kumar")
                .mobileNumber("9876543210")
                .build();
    }

    @Test
    @DisplayName("sendFamilyOtp should resolve HOF mobile, mask it, and send OTP successfully")
    void sendFamilyOtp_Success() {
        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0001", "pg.citya"))
                .thenReturn(Optional.of(activeFamily));
        when(userClient.searchUsersByUuids(eq(Collections.singletonList("head-uuid-1")), any()))
                .thenReturn(Collections.singletonMap("head-uuid-1", headProfile));
        when(otpClient.sendOtp(eq("9876543210"), eq("pg.citya"), any()))
                .thenReturn("REF-OTP-9988");

        FamilyOtpRequest request = FamilyOtpRequest.builder()
                .requestInfo(requestInfo)
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .build();

        FamilyOtpResponse response = familyOtpService.sendFamilyOtp(request);

        assertNotNull(response);
        assertEquals("FAM-2026-0001", response.getFamilyId());
        assertEquals("******3210", response.getMaskedMobileNumber());
        assertEquals("REF-OTP-9988", response.getReferenceId());
        assertTrue(response.getIsSent());
        verify(otpClient).sendOtp(eq("9876543210"), eq("pg.citya"), any());
    }

    @Test
    @DisplayName("sendFamilyOtp should throw FAMILY_NOT_FOUND when family does not exist")
    void sendFamilyOtp_FamilyNotFound_ThrowsException() {
        when(familyRepository.findByFamilyIdAndTenantId("NON-EXISTENT-FAM", "pg.citya"))
                .thenReturn(Optional.empty());

        FamilyOtpRequest request = FamilyOtpRequest.builder()
                .requestInfo(requestInfo)
                .familyId("NON-EXISTENT-FAM")
                .tenantId("pg.citya")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> familyOtpService.sendFamilyOtp(request));
        assertEquals(FamilyConstants.ERROR_FAMILY_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("sendFamilyOtp should throw INVALID_REQUEST when family is not active")
    void sendFamilyOtp_InactiveFamily_ThrowsException() {
        activeFamily.setStatus(FamilyStatus.INACTIVE);
        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0001", "pg.citya"))
                .thenReturn(Optional.of(activeFamily));

        FamilyOtpRequest request = FamilyOtpRequest.builder()
                .requestInfo(requestInfo)
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> familyOtpService.sendFamilyOtp(request));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("verifyFamilyOtp should validate OTP, enrich family details, register consent token, and return details")
    void verifyFamilyOtp_Success() {
        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0001", "pg.citya"))
                .thenReturn(Optional.of(activeFamily));
        when(userClient.searchUsersByUuids(eq(Collections.singletonList("head-uuid-1")), any()))
                .thenReturn(Collections.singletonMap("head-uuid-1", headProfile));
        when(otpClient.validateOtp(eq("9876543210"), eq("123456"), eq("pg.citya"), any()))
                .thenReturn(true);

        FamilyOtpVerifyRequest request = FamilyOtpVerifyRequest.builder()
                .requestInfo(requestInfo)
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .otp("123456")
                .referenceId("REF-OTP-9988")
                .build();

        FamilyOtpVerifyResponse response = familyOtpService.verifyFamilyOtp(request);

        assertNotNull(response);
        assertTrue(response.getIsVerified());
        assertNotNull(response.getFamily());
        assertEquals("FAM-2026-0001", response.getFamily().getFamilyId());
        assertNotNull(response.getConsentToken());
        assertTrue(response.getConsentToken().startsWith("CONSENT-VERIFIED-"));

        verify(requesterDxClient).registerConsentToken(eq(response.getConsentToken()), eq("FAM-2026-0001"), eq("applicant-uuid"));
    }

    @Test
    @DisplayName("verifyFamilyOtp should throw INVALID_AADHAAR_CONSENT when OTP is invalid")
    void verifyFamilyOtp_InvalidOtp_ThrowsException() {
        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0001", "pg.citya"))
                .thenReturn(Optional.of(activeFamily));
        when(userClient.searchUsersByUuids(eq(Collections.singletonList("head-uuid-1")), any()))
                .thenReturn(Collections.singletonMap("head-uuid-1", headProfile));
        when(otpClient.validateOtp(eq("9876543210"), eq("000000"), eq("pg.citya"), any()))
                .thenReturn(false);

        FamilyOtpVerifyRequest request = FamilyOtpVerifyRequest.builder()
                .requestInfo(requestInfo)
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .otp("000000")
                .referenceId("REF-OTP-9988")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> familyOtpService.verifyFamilyOtp(request));
        assertEquals(FamilyConstants.ERROR_CONSENT_INVALID, ex.getCode());
    }
}
