package org.egov.user.domain.service.profile;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.service.identity.IdentityVerificationService;
import org.egov.user.domain.service.profile.validator.ProfileValidator;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FamilyRestRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.profile.AadhaarHofCheckRequest;
import org.egov.user.web.contract.profile.AadhaarHofCheckResponse;
import org.egov.user.web.contract.profile.FamilyHofStatus;
import org.egov.user.web.controller.ProfileController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying Aadhaar Head of Family (HoF) check endpoint and service logic.
 *
 * Validates:
 * 1. Request validation (null request, blank Aadhaar, invalid Aadhaar format)
 * 2. Citizen presence check in eg_user (not found, inactive)
 * 3. Family mapping check via upyog-family-service (no family found)
 * 4. HoF verification check (citizen is member but not HoF)
 * 5. Successful HoF verification (via headUserUuid and via member.isHead)
 * 6. ProfileController endpoint /aadhaar/_checkHof integration
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AadhaarHofCheckTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileValidator profileValidator;

    @Mock
    private IdentityVerificationService identityVerificationService;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserDocumentRepository userDocumentRepository;

    @Mock
    private FamilyRestRepository familyRestRepository;

    private ProfileServiceImpl profileService;
    private ProfileController profileController;
    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(
                userRepository,
                profileValidator,
                identityVerificationService,
                encryptionDecryptionUtil,
                addressRepository,
                userDocumentRepository,
                null,
                null,
                null,
                null,
                familyRestRepository
        );

        when(encryptionDecryptionUtil.encryptObject(any(), any(), any())).thenAnswer(i -> i.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), any())).thenAnswer(i -> i.getArgument(0));

        profileController = new ProfileController(profileService, null);
        requestInfo = RequestInfo.builder().apiId("rainmaker").build();
    }

    @Test
    @DisplayName("checkAadhaarHof: Throws CustomException when request is null")
    void checkAadhaarHof_NullRequest_ThrowsException() {
        CustomException ex = assertThrows(CustomException.class, () -> profileService.checkAadhaarHof(null));
        assertEquals(UserConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("checkAadhaarHof: Throws CustomException when RequestInfo is null")
    void checkAadhaarHof_NullRequestInfo_ThrowsException() {
        AadhaarHofCheckRequest request = AadhaarHofCheckRequest.builder()
                .requestInfo(null)
                .aadhaarNumber("123456789012")
                .tenantId("pg.citya")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> profileService.checkAadhaarHof(request));
        assertEquals(UserConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("checkAadhaarHof: Throws CustomException when Aadhaar number is blank")
    void checkAadhaarHof_BlankAadhaar_ThrowsException() {
        AadhaarHofCheckRequest request = AadhaarHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .aadhaarNumber("  ")
                .tenantId("pg.citya")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> profileService.checkAadhaarHof(request));
        assertEquals(UserConstants.ERROR_AADHAAR_NUMBER_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("checkAadhaarHof: Throws CustomException when Aadhaar format is invalid")
    void checkAadhaarHof_InvalidAadhaarFormat_ThrowsException() {
        AadhaarHofCheckRequest request = AadhaarHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .aadhaarNumber("12345") // Less than 12 digits
                .tenantId("pg.citya")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> profileService.checkAadhaarHof(request));
        assertEquals(UserConstants.ERROR_INVALID_AADHAAR_FORMAT, ex.getCode());
        assertEquals(UserConstants.MSG_INVALID_AADHAAR_FORMAT, ex.getMessage());
    }

    @Test
    @DisplayName("checkAadhaarHof: Throws CustomException when citizen not found in eg_user")
    void checkAadhaarHof_CitizenNotFound_ThrowsException() {
        AadhaarHofCheckRequest request = AadhaarHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .aadhaarNumber("123456789012")
                .tenantId("pg.citya")
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        CustomException ex = assertThrows(CustomException.class, () -> profileService.checkAadhaarHof(request));
        assertEquals(UserConstants.ERROR_CITIZEN_NOT_FOUND, ex.getCode());
        assertEquals(UserConstants.MSG_CITIZEN_NOT_FOUND_FOR_AADHAAR, ex.getMessage());
    }

    @Test
    @DisplayName("checkAadhaarHof: Throws CustomException when citizen account is inactive")
    void checkAadhaarHof_CitizenInactive_ThrowsException() {
        AadhaarHofCheckRequest request = AadhaarHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .aadhaarNumber("123456789012")
                .tenantId("pg.citya")
                .build();

        User inactiveCitizen = User.builder()
                .uuid("citizen-inactive-uuid")
                .name("Inactive Citizen")
                .aadhaarNumber("123456789012")
                .active(false)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(inactiveCitizen));

        CustomException ex = assertThrows(CustomException.class, () -> profileService.checkAadhaarHof(request));
        assertEquals(UserConstants.ERROR_ACCOUNT_INACTIVE, ex.getCode());
    }

    @Test
    @DisplayName("checkAadhaarHof: Throws CustomException when citizen is not mapped to any family")
    void checkAadhaarHof_NoFamilyMapped_ThrowsException() {
        AadhaarHofCheckRequest request = AadhaarHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .aadhaarNumber("123456789012")
                .tenantId("pg.citya")
                .build();

        User activeCitizen = User.builder()
                .uuid("citizen-active-uuid")
                .name("Ramesh Kumar")
                .aadhaarNumber("123456789012")
                .active(true)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(activeCitizen));

        FamilyHofStatus noFamily = FamilyHofStatus.builder().familyFound(false).isHof(false).build();
        when(familyRestRepository.checkUserHofStatus(eq("citizen-active-uuid"), any(), any()))
                .thenReturn(noFamily);

        CustomException ex = assertThrows(CustomException.class, () -> profileService.checkAadhaarHof(request));
        assertEquals(UserConstants.ERROR_FAMILY_NOT_MAPPED, ex.getCode());
        assertEquals(UserConstants.MSG_CITIZEN_NOT_MAPPED_TO_FAMILY, ex.getMessage());
    }

    @Test
    @DisplayName("checkAadhaarHof: Throws CustomException when citizen is in family but NOT Head of Family")
    void checkAadhaarHof_CitizenNotHof_ThrowsException() {
        AadhaarHofCheckRequest request = AadhaarHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .aadhaarNumber("123456789012")
                .tenantId("pg.citya")
                .build();

        User activeCitizen = User.builder()
                .uuid("citizen-member-uuid")
                .name("Suresh Kumar")
                .aadhaarNumber("123456789012")
                .active(true)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(activeCitizen));

        FamilyHofStatus notHof = FamilyHofStatus.builder()
                .familyFound(true)
                .familyId("FAM-2026-0001")
                .headUserUuid("other-head-uuid")
                .isHof(false)
                .build();

        when(familyRestRepository.checkUserHofStatus(eq("citizen-member-uuid"), any(), any()))
                .thenReturn(notHof);

        CustomException ex = assertThrows(CustomException.class, () -> profileService.checkAadhaarHof(request));
        assertEquals(UserConstants.ERROR_NOT_HEAD_OF_FAMILY, ex.getCode());
        assertEquals(UserConstants.MSG_CITIZEN_NOT_HEAD_OF_FAMILY, ex.getMessage());
    }

    @Test
    @DisplayName("checkAadhaarHof: Returns success response when citizen is confirmed Head of Family")
    void checkAadhaarHof_CitizenIsHof_ReturnsSuccess() {
        AadhaarHofCheckRequest request = AadhaarHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .aadhaarNumber("123456789012")
                .tenantId("pg.citya")
                .build();

        User activeCitizen = User.builder()
                .uuid("citizen-head-uuid")
                .name("Ramesh Kumar")
                .aadhaarNumber("123456789012")
                .active(true)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(activeCitizen));

        FamilyHofStatus hofStatus = FamilyHofStatus.builder()
                .familyFound(true)
                .familyId("FAM-2026-0001")
                .headUserUuid("citizen-head-uuid")
                .isHof(true)
                .build();

        when(familyRestRepository.checkUserHofStatus(eq("citizen-head-uuid"), any(), any()))
                .thenReturn(hofStatus);

        AadhaarHofCheckResponse response = profileService.checkAadhaarHof(request);

        assertNotNull(response);
        assertTrue(response.getIsHof());
        assertEquals("citizen-head-uuid", response.getUserUuid());
        assertEquals("Ramesh Kumar", response.getUserName());
        assertEquals("FAM-2026-0001", response.getFamilyId());
        assertEquals("XXXXXXXX9012", response.getMaskedAadhaar());
        assertEquals("pg", response.getTenantId());
        assertEquals(UserConstants.MSG_CITIZEN_HOF_VERIFIED_SUCCESS, response.getMessage());
    }

    @Test
    @DisplayName("Controller: POST /aadhaar/_checkHof returns HTTP 200 and response envelope")
    void controller_CheckAadhaarHof_ReturnsHttp200() {
        AadhaarHofCheckRequest request = AadhaarHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .aadhaarNumber("123456789012")
                .tenantId("pg.citya")
                .build();

        User activeCitizen = User.builder()
                .uuid("citizen-head-uuid")
                .name("Ramesh Kumar")
                .aadhaarNumber("123456789012")
                .active(true)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(activeCitizen));

        FamilyHofStatus hofStatus = FamilyHofStatus.builder()
                .familyFound(true)
                .familyId("FAM-2026-0001")
                .headUserUuid("citizen-head-uuid")
                .isHof(true)
                .build();

        when(familyRestRepository.checkUserHofStatus(eq("citizen-head-uuid"), any(), any()))
                .thenReturn(hofStatus);

        ResponseEntity<AadhaarHofCheckResponse> responseEntity = profileController.checkAadhaarHof(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().getIsHof());
        assertEquals("FAM-2026-0001", responseEntity.getBody().getFamilyId());
        assertEquals("citizen-head-uuid", responseEntity.getBody().getUserUuid());
    }
}
