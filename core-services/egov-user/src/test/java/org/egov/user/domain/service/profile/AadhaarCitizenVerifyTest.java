package org.egov.user.domain.service.profile;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.identity.IdentityVerificationService;
import org.egov.user.domain.service.identity.VerifiedAadhaarDemographics;
import org.egov.user.domain.service.profile.validator.ProfileValidator;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FamilyRestRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.profile.AadhaarCitizenVerifyRequest;
import org.egov.user.web.contract.profile.AadhaarCitizenVerifyResponse;
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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying Aadhaar OTP verification and citizen identity resolution
 * (Scenario 1: Existing citizen lookup, Scenario 2: New citizen auto-provisioning).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AadhaarCitizenVerifyTest {

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
    }

    private Date parseDate(String dateStr) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        } catch (Exception e) {
            return new Date(1700000000000L);
        }
    }

    @Test
    @DisplayName("Scenario 1: Existing citizen with matching Aadhaar should return full info, familyId and isNewUser=false")
    void testVerifyAadhaarExistingUser_Success() {
        String refId = "298662";
        String otp = "123456";
        String aadhaar = "123456789012";
        String maskedAadhaar = "XXXXXXXX9012";
        String userUuid = "existing-user-uuid-101";

        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar(maskedAadhaar)
                .name("Rohan Sharma")
                .gender(Gender.MALE)
                .dob(parseDate("1992-04-15"))
                .mobileNumber("9876543210")
                .verified(true)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), eq("pg"), eq(refId), eq(aadhaar), eq(otp), eq(false)))
                .thenReturn(eKyc);

        User existingUser = User.builder()
                .id(101L)
                .uuid(userUuid)
                .username("9876543210")
                .name("Rohan Sharma")
                .mobileNumber("9876543210")
                .gender(Gender.MALE)
                .dob(parseDate("1992-04-15"))
                .aadhaarNumber(maskedAadhaar)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingUser));

        when(familyRestRepository.getActiveFamilyIdByUserUuid(eq(userUuid), eq("pg"), any()))
                .thenReturn("UPYOG-FAM-2024-001");

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().apiId("org.upyog.dx").build())
                .referenceId(refId)
                .otp(otp)
                .aadhaarNumber(aadhaar)
                .tenantId("pg")
                .build();

        AadhaarCitizenVerifyResponse response = profileService.verifyAndResolveAadhaarCitizen(request);

        assertNotNull(response);
        assertEquals(Boolean.FALSE, response.getIsNewUser());

        assertNotNull(response.getUser());
        assertEquals(userUuid, response.getUser().getUuid());
        assertEquals("Rohan Sharma", response.getUser().getName());
        assertEquals("9876543210", response.getUser().getMobileNumber());
        assertEquals(Gender.MALE.toString(), response.getUser().getGender());
        assertEquals(maskedAadhaar, response.getUser().getAadhaarNumber());
        assertEquals("UPYOG-FAM-2024-001", response.getUser().getFamilyId());
        assertTrue(response.getUser().getIsIdentityVerified());

        verify(userRepository, atLeastOnce()).update(any(), any(), eq(101L), eq(userUuid));
    }

    @Test
    @DisplayName("Scenario 1b: Existing citizen found via mobile lookup should link Aadhaar and return isNewUser=false")
    void testVerifyAadhaarExistingUser_FoundByMobile() {
        String refId = "ref-8822";
        String otp = "123456";
        String maskedAadhaar = "XXXXXXXX5544";
        String userUuid = "mobile-user-uuid-202";

        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar(maskedAadhaar)
                .name("Priya Verma")
                .gender(Gender.FEMALE)
                .dob(parseDate("1996-08-20"))
                .mobileNumber("9988776655")
                .verified(true)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), eq("pg"), eq(refId), isNull(), eq(otp), eq(false)))
                .thenReturn(eKyc);

        User existingUser = User.builder()
                .id(202L)
                .uuid(userUuid)
                .username("9988776655")
                .name("Priya Verma")
                .mobileNumber("9988776655")
                .gender(Gender.FEMALE)
                .dob(parseDate("1996-08-20"))
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenAnswer(invocation -> {
            UserSearchCriteria criteria = invocation.getArgument(0);
            if (criteria.getMobileNumber() != null || criteria.getUserName() != null) {
                return Collections.singletonList(existingUser);
            }
            return Collections.emptyList();
        });

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().apiId("org.upyog.dx").build())
                .referenceId(refId)
                .otp(otp)
                .tenantId("pg")
                .build();

        AadhaarCitizenVerifyResponse response = profileService.verifyAndResolveAadhaarCitizen(request);

        assertNotNull(response);
        assertEquals(Boolean.FALSE, response.getIsNewUser());
        assertNotNull(response.getUser());
        assertEquals("Priya Verma", response.getUser().getName());
        assertEquals("9988776655", response.getUser().getMobileNumber());
        assertEquals(maskedAadhaar, response.getUser().getAadhaarNumber());
        assertTrue(response.getUser().getIsIdentityVerified());
    }

    @Test
    @DisplayName("Scenario 1c: Existing verified citizen exists on mobile -> Shared mobile family member auto-provisions as new citizen")
    void testVerifyAadhaarSharedMobile_ExistingCitizenVerified_AutoProvisionsNewUser() {
        String refId = "ref-family-member";
        String otp = "123456";
        String newMemberAadhaar = "XXXXXXXX9999";
        String sharedMobile = "9876543210";

        // Incoming eKYC for new family member
        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar(newMemberAadhaar)
                .name("Son Sharma")
                .gender(Gender.MALE)
                .dob(parseDate("2005-06-15"))
                .mobileNumber(sharedMobile)
                .verified(true)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), eq("pg"), eq(refId), isNull(), eq(otp), eq(false)))
                .thenReturn(eKyc);

        // Father already exists on the same mobile and is verified with his own Aadhaar
        User fatherUser = User.builder()
                .id(101L)
                .uuid("father-uuid")
                .username("9876543210")
                .name("Father Sharma")
                .mobileNumber(sharedMobile)
                .gender(Gender.MALE)
                .dob(parseDate("1970-01-01"))
                .aadhaarNumber("XXXXXXXX1111")
                .isIdentityVerified(true)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenAnswer(invocation -> {
            UserSearchCriteria criteria = invocation.getArgument(0);
            if (newMemberAadhaar.equalsIgnoreCase(criteria.getAadhaarNumber())) {
                return Collections.emptyList(); // Son doesn't exist yet
            }
            if (sharedMobile.equals(criteria.getMobileNumber()) || sharedMobile.equals(criteria.getUserName())) {
                return Collections.singletonList(fatherUser); // Father found on this mobile
            }
            return Collections.emptyList();
        });

        when(userRepository.isUserPresent(anyString(), anyString(), any())).thenReturn(false);
        when(userRepository.create(any(User.class))).thenAnswer(invocation -> {
            User passed = invocation.getArgument(0);
            passed.setId(909L);
            return passed;
        });

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().apiId("org.upyog.dx").build())
                .referenceId(refId)
                .otp(otp)
                .mobileNumber(sharedMobile)
                .tenantId("pg")
                .build();

        AadhaarCitizenVerifyResponse response = profileService.verifyAndResolveAadhaarCitizen(request);

        assertNotNull(response);
        assertEquals(Boolean.TRUE, response.getIsNewUser(), "Must auto-provision new citizen without overwriting father's account");
        assertNotNull(response.getUser());
        assertEquals("Son Sharma", response.getUser().getName());
        assertEquals(sharedMobile, response.getUser().getMobileNumber());
        assertEquals(newMemberAadhaar, response.getUser().getAadhaarNumber());
        assertTrue(response.getUser().getIsIdentityVerified());
        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("Scenario 2: Citizen does not exist in eg_user -> Auto-provision new account and return info with isNewUser=true")
    void testVerifyAadhaarNewUser_AutoProvision() {
        String refId = "298662";
        String otp = "123456";
        String maskedAadhaar = "XXXXXXXX1234";

        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar(maskedAadhaar)
                .name("New Member")
                .gender(Gender.FEMALE)
                .dob(parseDate("2000-01-01"))
                .mobileNumber("9123456789")
                .verified(true)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), eq("pg"), eq(refId), isNull(), eq(otp), eq(false)))
                .thenReturn(eKyc);

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());
        when(userRepository.isUserPresent(anyString(), anyString(), any())).thenReturn(false);

        when(userRepository.create(any(User.class))).thenAnswer(invocation -> {
            User passed = invocation.getArgument(0);
            passed.setId(505L);
            return passed;
        });

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().apiId("org.upyog.dx").build())
                .referenceId(refId)
                .otp(otp)
                .tenantId("pg")
                .build();

        AadhaarCitizenVerifyResponse response = profileService.verifyAndResolveAadhaarCitizen(request);

        assertNotNull(response);
        assertEquals(Boolean.TRUE, response.getIsNewUser());

        assertNotNull(response.getUser());
        assertEquals("New Member", response.getUser().getName());
        assertEquals("9123456789", response.getUser().getMobileNumber());
        assertEquals(Gender.FEMALE.toString(), response.getUser().getGender());
        assertEquals(maskedAadhaar, response.getUser().getAadhaarNumber());
        assertNull(response.getUser().getFamilyId());
        assertNotNull(response.getUser().getUuid());
        assertTrue(response.getUser().getIsIdentityVerified());

        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("Should throw CustomException when OTP is missing")
    void testVerifyAadhaar_MissingOtp_ThrowsException() {
        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .referenceId("298662")
                .tenantId("pg")
                .build();

        assertThrows(CustomException.class, () -> profileService.verifyAndResolveAadhaarCitizen(request));
    }

    @Test
    @DisplayName("Should throw CustomException when both referenceId and aadhaarNumber are missing")
    void testVerifyAadhaar_MissingIdentifiers_ThrowsException() {
        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .otp("123456")
                .tenantId("pg")
                .build();

        assertThrows(CustomException.class, () -> profileService.verifyAndResolveAadhaarCitizen(request));
    }

    @Test
    @DisplayName("ProfileController endpoint /profile/aadhaar/_verify should delegate to profileService")
    void testProfileControllerEndpoint() {
        ProfileController controller = new ProfileController(profileService);

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .referenceId("ref-123")
                .otp("123456")
                .tenantId("pg")
                .build();

        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar("XXXXXXXX7788")
                .name("Controller Test User")
                .gender(Gender.MALE)
                .dob(parseDate("1990-01-01"))
                .verified(true)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(eKyc);
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());
        when(userRepository.create(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(901L);
            return u;
        });

        ResponseEntity<AadhaarCitizenVerifyResponse> respEntity = controller.verifyAadhaarCitizen(request);

        assertNotNull(respEntity);
        assertEquals(HttpStatus.OK, respEntity.getStatusCode());
        assertNotNull(respEntity.getBody());
        assertEquals("Controller Test User", respEntity.getBody().getUser().getName());
    }

    @Test
    @DisplayName("Should successfully verify logged-in citizen profile when Aadhaar mobile matches logged-in mobile")
    void testVerifyLoggedInCitizenProfile_Success_MatchingMobile() {
        String citizenUuid = "citizen-uuid-001";
        String mobile = "9876543210";

        org.egov.common.contract.request.User callerUser = org.egov.common.contract.request.User.builder()
                .uuid(citizenUuid)
                .userName(mobile)
                .mobileNumber(mobile)
                .tenantId("pg.citya")
                .id(101L)
                .build();

        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(callerUser)
                .build();

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(requestInfo)
                .referenceId("ref-kyc-001")
                .otp("123456")
                .tenantId("pg.citya")
                .isSelfVerification(true)
                .build();

        User existingDbUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .username(mobile)
                .mobileNumber(mobile)
                .name("Unverified Citizen")
                .active(true)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .isIdentityVerified(false)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingDbUser));

        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar("XXXXXXXX9988")
                .name("Aadhaar Verified Citizen")
                .gender(Gender.MALE)
                .dob(parseDate("1992-05-15"))
                .mobileNumber("+919876543210")
                .verified(true)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), any(), eq("ref-kyc-001"), any(), eq("123456"), eq(false)))
                .thenReturn(eKyc);

        AadhaarCitizenVerifyResponse response = profileService.verifyLoggedInCitizenProfile(request);

        assertNotNull(response);
        assertFalse(response.getIsNewUser());
        assertNotNull(response.getUser());
        assertEquals("Aadhaar Verified Citizen", response.getUser().getName());
        assertEquals(Boolean.TRUE, response.getUser().getIsIdentityVerified());
        assertEquals("AADHAAR", response.getUser().getIdentityVerifiedSource());
        assertEquals("XXXXXXXX9988", response.getUser().getAadhaarNumber());

        verify(userRepository, times(1)).update(any(User.class), any(User.class), eq(101L), eq(citizenUuid));
    }

    @Test
    @DisplayName("Should throw AADHAAR_MOBILE_MISMATCH when Aadhaar registered mobile differs from logged-in mobile")
    void testVerifyLoggedInCitizenProfile_MismatchMobile_ThrowsException() {
        String citizenUuid = "citizen-uuid-002";
        String mobile = "9876543210";

        org.egov.common.contract.request.User callerUser = org.egov.common.contract.request.User.builder()
                .uuid(citizenUuid)
                .userName(mobile)
                .mobileNumber(mobile)
                .tenantId("pg.citya")
                .id(102L)
                .build();

        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(callerUser)
                .build();

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(requestInfo)
                .referenceId("ref-kyc-002")
                .otp("123456")
                .tenantId("pg.citya")
                .build();

        User existingDbUser = User.builder()
                .id(102L)
                .uuid(citizenUuid)
                .username(mobile)
                .mobileNumber(mobile)
                .name("Mobile Citizen")
                .active(true)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .isIdentityVerified(false)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingDbUser));

        // Aadhaar has a completely different mobile number
        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar("XXXXXXXX4321")
                .name("Other Citizen")
                .gender(Gender.FEMALE)
                .dob(parseDate("1985-08-20"))
                .mobileNumber("9111223344")
                .verified(true)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), any(), eq("ref-kyc-002"), any(), eq("123456"), eq(false)))
                .thenReturn(eKyc);

        CustomException ce = assertThrows(CustomException.class, () -> profileService.verifyLoggedInCitizenProfile(request));
        assertEquals("AADHAAR_MOBILE_MISMATCH", ce.getCode());
        verify(userRepository, never()).update(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("Should throw AADHAAR_MOBILE_MISMATCH when Aadhaar identity provider returns null mobileNumber")
    void testVerifyLoggedInCitizenProfile_MissingAadhaarMobile_ThrowsException() {
        String citizenUuid = "citizen-uuid-003";
        String mobile = "9876543210";

        org.egov.common.contract.request.User callerUser = org.egov.common.contract.request.User.builder()
                .uuid(citizenUuid)
                .userName(mobile)
                .mobileNumber(mobile)
                .tenantId("pg.citya")
                .id(103L)
                .build();

        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(callerUser)
                .build();

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(requestInfo)
                .referenceId("ref-kyc-003")
                .otp("123456")
                .tenantId("pg.citya")
                .build();

        User existingDbUser = User.builder()
                .id(103L)
                .uuid(citizenUuid)
                .username(mobile)
                .mobileNumber(mobile)
                .name("Mobile Citizen 3")
                .active(true)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingDbUser));

        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar("XXXXXXXX4321")
                .name("Citizen Without Mobile In Demographics")
                .verified(true)
                .mobileNumber(null)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), any(), eq("ref-kyc-003"), any(), eq("123456"), eq(false)))
                .thenReturn(eKyc);

        CustomException ce = assertThrows(CustomException.class, () -> profileService.verifyLoggedInCitizenProfile(request));
        assertEquals("AADHAAR_MOBILE_MISMATCH", ce.getCode());
        verify(userRepository, never()).update(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED_ACCESS when caller UserInfo is missing")
    void testVerifyLoggedInCitizenProfile_Unauthenticated_ThrowsException() {
        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .referenceId("ref-kyc-004")
                .otp("123456")
                .tenantId("pg.citya")
                .build();

        CustomException ce = assertThrows(CustomException.class, () -> profileService.verifyLoggedInCitizenProfile(request));
        assertEquals("UNAUTHORIZED_ACCESS", ce.getCode());
    }

    @Test
    @DisplayName("ProfileController endpoint /profile/aadhaar/_verifyProfile should call verifyLoggedInCitizenProfile")
    void testProfileControllerVerifyProfileEndpoint() {
        ProfileController controller = new ProfileController(profileService);

        String citizenUuid = "citizen-uuid-ctrl";
        String mobile = "9876543210";

        org.egov.common.contract.request.User callerUser = org.egov.common.contract.request.User.builder()
                .uuid(citizenUuid)
                .userName(mobile)
                .mobileNumber(mobile)
                .tenantId("pg.citya")
                .id(105L)
                .build();

        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(callerUser)
                .build();

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(requestInfo)
                .referenceId("ref-ctrl-001")
                .otp("123456")
                .tenantId("pg.citya")
                .build();

        User existingDbUser = User.builder()
                .id(105L)
                .uuid(citizenUuid)
                .username(mobile)
                .mobileNumber(mobile)
                .name("Controller Citizen")
                .active(true)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingDbUser));

        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar("XXXXXXXX8888")
                .name("Controller Verified User")
                .mobileNumber(mobile)
                .gender(Gender.MALE)
                .dob(parseDate("1992-05-15"))
                .verified(true)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(eKyc);

        ResponseEntity<AadhaarCitizenVerifyResponse> respEntity = controller.verifyProfileWithAadhaar(request);

        assertNotNull(respEntity);
        assertEquals(HttpStatus.OK, respEntity.getStatusCode());
        assertNotNull(respEntity.getBody());
        assertEquals("Controller Verified User", respEntity.getBody().getUser().getName());
        assertEquals(Boolean.TRUE, respEntity.getBody().getUser().getIsIdentityVerified());
    }

    @Test
    @DisplayName("Should match mobile and succeed when Aadhaar mock is enabled even if eKyc mobile is null")
    void shouldMatchMobileAndSucceedWhenAadhaarMockEnabledEvenIfEkycMobileIsNull() {
        String citizenUuid = "citizen-mock-uuid-999";
        String mobile = "9876543210";

        UserConfiguration userConfig = mock(UserConfiguration.class);
        when(userConfig.getAadhaarGatewayMockEnabled()).thenReturn(true);
        profileService.setUserConfiguration(userConfig);

        org.egov.common.contract.request.User callerInfo = org.egov.common.contract.request.User.builder()
                .uuid(citizenUuid)
                .userName(mobile)
                .mobileNumber(mobile)
                .build();

        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("org.egov.user")
                .ver("1.0")
                .userInfo(callerInfo)
                .build();

        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(requestInfo)
                .referenceId("ref-mock-999")
                .otp("123456")
                .aadhaarNumber("548796521452")
                .tenantId("pg.citya")
                .build();

        User existingDbUser = User.builder()
                .id(999L)
                .uuid(citizenUuid)
                .username(mobile)
                .mobileNumber(mobile)
                .name("Mock Citizen")
                .active(true)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingDbUser));

        VerifiedAadhaarDemographics eKyc = VerifiedAadhaarDemographics.builder()
                .maskedAadhaar("XXXXXXXX1452")
                .name("Aadhaar Mock User")
                .mobileNumber(null)
                .gender(Gender.MALE)
                .dob(parseDate("1995-05-15"))
                .verified(true)
                .build();

        when(identityVerificationService.verifyAadhaarOtp(any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(eKyc);

        AadhaarCitizenVerifyResponse response = profileService.verifyLoggedInCitizenProfile(request);

        assertNotNull(response);
        assertNotNull(response.getUser());
        assertEquals("Aadhaar Mock User", response.getUser().getName());
        assertEquals(Boolean.TRUE, response.getUser().getIsIdentityVerified());
        assertEquals(mobile, eKyc.getMobileNumber());
    }
}
