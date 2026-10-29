package org.egov.user.domain.service.profile;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.identity.IdentityVerificationService;
import org.egov.user.domain.service.profile.validator.ProfileValidator;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.profile.ProfileCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying multi-user profile creation under the same mobile number (e.g. family members)
 * and duplicate profile prevention when Aadhaar or Name+DOB match.
 */
@ExtendWith(MockitoExtension.class)
public class ProfileMultiUserCreationTest {

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

    private ProfileServiceImpl profileService;

    @BeforeEach
    public void setUp() {
        profileService = new ProfileServiceImpl(
                userRepository,
                profileValidator,
                identityVerificationService,
                encryptionDecryptionUtil,
                addressRepository,
                userDocumentRepository
        );
    }

    private Date createDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private ProfileCreateRequest buildCreateRequest(String name, String mobile, Date dob, String aadhaar) {
        Address address = Address.builder()
                .city("Amritsar")
                .locality("Green Avenue")
                .streetName("Mall Road")
                .houseNumber("12-B")
                .landmark("Near Golden Temple")
                .pinCode("143001")
                .type(AddressType.PERMANENT)
                .build();

        return ProfileCreateRequest.builder()
                .name(name)
                .fatherName("Parent Name")
                .motherName("Mother Name")
                .mobileNumber(mobile)
                .dob(dob)
                .emailId(name.toLowerCase().replace(" ", ".") + "@example.com")
                .gender("MALE")
                .tenantId("pb.amritsar")
                .aadhaarNumber(aadhaar)
                .address(address)
                .requestInfo(new RequestInfo())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a second profile with the same mobile number using a disambiguated username")
    public void testCreateProfile_WhenSameMobileDifferentCitizen_SucceedsWithDisambiguatedUsername() {
        Date fatherDob = createDate(1965, 5, 20);
        Date sonDob = createDate(1995, 10, 15);

        User existingFather = User.builder()
                .id(101L)
                .uuid("uuid-father")
                .username("9876543210")
                .mobileNumber("9876543210")
                .name("Ravi Kumar")
                .dob(fatherDob)
                .aadhaarNumber("XXXXXXXX1111")
                .gender(Gender.MALE)
                .tenantId("pb")
                .build();

        ProfileCreateRequest sonRequest = buildCreateRequest("Aarav Kumar", "9876543210", sonDob, "XXXXXXXX2222");

        when(encryptionDecryptionUtil.encryptObject(any(UserSearchCriteria.class), eq("User"), eq(UserSearchCriteria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenAnswer(invocation -> {
            UserSearchCriteria crit = invocation.getArgument(0);
            if (crit.getAadhaarNumber() != null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(existingFather);
        });
        when(encryptionDecryptionUtil.decryptObject(any(), any(), eq(User.class), any()))
                .thenReturn(Collections.singletonList(existingFather));

        // Mobile is already taken as a username by father
        when(userRepository.isUserPresent(eq("9876543210"), eq("pb"), eq(UserType.CITIZEN))).thenReturn(true);

        when(encryptionDecryptionUtil.encryptObject(any(User.class), eq("User"), eq(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.create(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        User createdUser = profileService.createProfile(sonRequest);

        assertNotNull(createdUser);
        User captured = userCaptor.getValue();
        assertEquals("Aarav Kumar", captured.getName());
        assertEquals("9876543210", captured.getMobileNumber());
        assertTrue(captured.getUsername().startsWith("9876543210_"), "Username should be disambiguated with a suffix");
        assertNotEquals("9876543210", captured.getUsername());
    }

    @Test
    @DisplayName("Should throw USER_ALREADY_EXISTS when another profile on the same mobile has identical name and DOB")
    public void testCreateProfile_WhenSameMobileSameNameAndDob_ThrowsUserAlreadyExists() {
        Date dob = createDate(1990, 8, 15);

        User existing = User.builder()
                .id(101L)
                .uuid("uuid-existing")
                .username("9876543210")
                .mobileNumber("9876543210")
                .name("Aarav Sharma")
                .dob(dob)
                .aadhaarNumber("XXXXXXXX1111")
                .build();

        ProfileCreateRequest duplicateRequest = buildCreateRequest("Aarav Sharma", "9876543210", dob, null);

        when(encryptionDecryptionUtil.encryptObject(any(UserSearchCriteria.class), eq("User"), eq(UserSearchCriteria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existing));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), eq(User.class), any()))
                .thenReturn(Collections.singletonList(existing));

        CustomException ex = assertThrows(CustomException.class, () -> profileService.createProfile(duplicateRequest));
        assertEquals("USER_ALREADY_EXISTS", ex.getCode());
        assertTrue(ex.getMessage().contains("date of birth"));
    }

    @Test
    @DisplayName("Should throw USER_ALREADY_EXISTS when another profile in tenant matches the same Aadhaar number")
    public void testCreateProfile_WhenSameAadhaar_ThrowsUserAlreadyExists() {
        Date dob1 = createDate(1990, 8, 15);
        Date dob2 = createDate(1992, 1, 10);

        User existing = User.builder()
                .id(101L)
                .uuid("uuid-existing")
                .username("9876543210")
                .mobileNumber("9876543210")
                .name("Aarav Sharma")
                .dob(dob1)
                .aadhaarNumber("123456789012")
                .build();

        ProfileCreateRequest duplicateAadhaarRequest = buildCreateRequest("Other Person", "9876543210", dob2, "123456789012");

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existing));

        CustomException ex = assertThrows(CustomException.class, () -> profileService.createProfile(duplicateAadhaarRequest));
        assertEquals(UserConstants.ERROR_AADHAAR_ALREADY_LINKED, ex.getCode());
        assertTrue(ex.getMessage().contains("Aadhaar number"));
    }
}
