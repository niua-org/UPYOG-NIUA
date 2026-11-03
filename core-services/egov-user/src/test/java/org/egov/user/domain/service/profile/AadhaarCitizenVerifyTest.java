package org.egov.user.domain.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
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
public class AadhaarCitizenVerifyTest {

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
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
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
        objectMapper = new ObjectMapper();
        when(encryptionDecryptionUtil.encryptObject(any(), any(), any())).thenAnswer(i -> i.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), any())).thenAnswer(i -> i.getArgument(0));
    }

    private Date parseDate(String dateStr) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        } catch (Exception e) {
            return new Date();
        }
    }

    @Test
    @DisplayName("Scenario 1: Existing citizen with matching Aadhaar should return full info, familyId and isNewUser=false")
    public void testVerifyAadhaarExistingUser_Success() {
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
    public void testVerifyAadhaarExistingUser_FoundByMobile() {
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
    @DisplayName("Scenario 2: Citizen does not exist in eg_user -> Auto-provision new account and return info with isNewUser=true")
    public void testVerifyAadhaarNewUser_AutoProvision() {
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
    public void testVerifyAadhaar_MissingOtp_ThrowsException() {
        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .referenceId("298662")
                .tenantId("pg")
                .build();

        assertThrows(CustomException.class, () -> profileService.verifyAndResolveAadhaarCitizen(request));
    }

    @Test
    @DisplayName("Should throw CustomException when both referenceId and aadhaarNumber are missing")
    public void testVerifyAadhaar_MissingIdentifiers_ThrowsException() {
        AadhaarCitizenVerifyRequest request = AadhaarCitizenVerifyRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .otp("123456")
                .tenantId("pg")
                .build();

        assertThrows(CustomException.class, () -> profileService.verifyAndResolveAadhaarCitizen(request));
    }

    @Test
    @DisplayName("ProfileController endpoint /profile/aadhaar/_verify should delegate to profileService")
    public void testProfileControllerEndpoint() {
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
                .dob(new Date())
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
}
