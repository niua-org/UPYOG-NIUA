package org.egov.user.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.exception.DuplicateUserNameException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FileStoreRepository;
import org.egov.user.persistence.repository.OtpRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Test suite verifying mobile number and account uniqueness rules during user creation in {@link UserService}.
 * <p>
 * Key rules verified:
 * <ul>
 *   <li><b>Employee Accounts:</b>
 *     <ul>
 *       <li>An employee can be created with a mobile number that is already used by a citizen account.</li>
 *       <li>Only one employee account can exist per mobile number in a tenant; duplicate employee mobile is rejected.</li>
 *     </ul>
 *   </li>
 *   <li><b>Citizen Accounts:</b>
 *     <ul>
 *       <li>Multiple citizens can share a mobile number (family multi-account).</li>
 *       <li>Only one unverified citizen account is permitted per mobile number.</li>
 *       <li>Multiple verified citizen accounts (e.g. Aadhaar verified) can share the mobile number with unique Aadhaar numbers.</li>
 *     </ul>
 *   </li>
 *   <li><b>No Silent Merge:</b> New user requests always create separate user records rather than silently updating old ones.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserCreateDeduplicationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private FileStoreRepository fileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @Mock
    private AddressRepository addressRepository;

    private UserService userService;

    @BeforeEach
    public void setUp() {
        userService = new UserService(
                userRepository,
                otpRepository,
                fileRepository,
                passwordEncoder,
                encryptionDecryptionUtil,
                90,
                true,
                false,
                null,
                50,
                4,
                addressRepository
        );

        when(encryptionDecryptionUtil.encryptObject(any(), any(), any())).thenAnswer(i -> i.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), any())).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hashed-pwd");
    }

    private RequestInfo createSampleRequestInfo() {
        return RequestInfo.builder()
                .apiId("org.upyog.test")
                .ver("1.0")
                .ts(1729500000000L)
                .userInfo(org.egov.common.contract.request.User.builder().id(999L).uuid("updater-uuid").build())
                .build();
    }

    @Test
    @DisplayName("Employee creation: Allowed when a citizen already exists with the same mobile number")
    public void testCreateEmployee_AllowedWhenCitizenExistsOnSameMobile() {
        String mobile = "8588070881";

        // DB returns a citizen when queried for CITIZEN, but empty when queried for EMPLOYEE
        when(userRepository.findAll(argThat(criteria -> criteria != null && UserType.EMPLOYEE.equals(criteria.getType()))))
                .thenReturn(Collections.emptyList());

        User incomingEmployee = User.builder()
                .username("EMP_ADMIN")
                .mobileNumber(mobile)
                .name("Employee Admin")
                .tenantId("pg.citya")
                .type(UserType.EMPLOYEE)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("EMPLOYEE").tenantId("pg").build()))
                .build();

        User savedEmployee = incomingEmployee.toBuilder().id(202L).uuid("new-emp-uuid").build();
        when(userRepository.create(any(User.class))).thenReturn(savedEmployee);

        User result = userService.createUser(incomingEmployee, createSampleRequestInfo());

        assertNotNull(result);
        assertEquals(202L, result.getId());
        assertEquals("new-emp-uuid", result.getUuid());
        assertEquals(UserType.EMPLOYEE, result.getType());

        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("Employee creation: Rejected when another active employee already exists with the same mobile number")
    public void testCreateEmployee_ThrowsExceptionWhenAnotherEmployeeAlreadyExistsOnSameMobile() {
        String mobile = "8588070881";

        User existingEmployee = User.builder()
                .id(101L)
                .uuid("existing-emp-uuid")
                .username("EMP_EXISTING")
                .mobileNumber(mobile)
                .name("Existing Employee")
                .type(UserType.EMPLOYEE)
                .active(true)
                .tenantId("pg")
                .build();

        when(userRepository.findAll(argThat(criteria -> criteria != null && UserType.EMPLOYEE.equals(criteria.getType()))))
                .thenReturn(Collections.singletonList(existingEmployee));

        User incomingEmployee = User.builder()
                .username("EMP_NEW")
                .mobileNumber(mobile)
                .name("New Employee")
                .tenantId("pg")
                .type(UserType.EMPLOYEE)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("EMPLOYEE").tenantId("pg").build()))
                .build();

        CustomException exception = assertThrows(CustomException.class, () ->
                userService.createUser(incomingEmployee, createSampleRequestInfo())
        );

        assertEquals(UserConstants.ERROR_EMPLOYEE_ALREADY_EXISTS_WITH_MOBILE, exception.getCode());
        verify(userRepository, never()).create(any(User.class));
    }

    @Test
    @DisplayName("Citizen creation: Rejected when an unverified citizen already exists on the same mobile and incoming is unverified")
    public void testCreateCitizen_ThrowsExceptionWhenUnverifiedCitizenAlreadyExistsOnSameMobile() {
        String mobile = "8588070881";

        User existingUnverifiedCitizen = User.builder()
                .id(101L)
                .uuid("existing-unverified-uuid")
                .username(mobile)
                .mobileNumber(mobile)
                .name("Unverified Citizen")
                .type(UserType.CITIZEN)
                .isIdentityVerified(false)
                .active(true)
                .tenantId("pg")
                .build();

        when(userRepository.findAll(argThat(criteria -> criteria != null && UserType.CITIZEN.equals(criteria.getType()))))
                .thenReturn(Collections.singletonList(existingUnverifiedCitizen));

        User incomingUnverifiedCitizen = User.builder()
                .username("new_unverified_citizen")
                .mobileNumber(mobile)
                .name("New Unverified Citizen")
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .isIdentityVerified(false)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        CustomException exception = assertThrows(CustomException.class, () ->
                userService.createUser(incomingUnverifiedCitizen, createSampleRequestInfo())
        );

        assertEquals(UserConstants.ERROR_UNVERIFIED_CITIZEN_EXISTS, exception.getCode());
        verify(userRepository, never()).create(any(User.class));
    }

    @Test
    @DisplayName("Citizen creation: Allowed when unverified citizen exists on mobile but incoming citizen is identity verified with Aadhaar")
    public void testCreateCitizen_AllowedWhenIncomingCitizenIsIdentityVerifiedWithDistinctAadhaar() {
        String mobile = "8588070881";

        User existingUnverifiedCitizen = User.builder()
                .id(101L)
                .uuid("existing-unverified-uuid")
                .username(mobile)
                .mobileNumber(mobile)
                .name("Unverified Citizen")
                .type(UserType.CITIZEN)
                .isIdentityVerified(false)
                .active(true)
                .tenantId("pg")
                .build();

        when(userRepository.findAll(argThat(criteria -> criteria != null && UserType.CITIZEN.equals(criteria.getType()))))
                .thenReturn(Collections.singletonList(existingUnverifiedCitizen));

        User incomingVerifiedCitizen = User.builder()
                .username("verified_family_member")
                .mobileNumber(mobile)
                .name("Verified Member")
                .aadhaarNumber("XXXXXXXX2222")
                .isIdentityVerified(true)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        User savedUser = incomingVerifiedCitizen.toBuilder().id(303L).uuid("new-verified-uuid").build();
        when(userRepository.create(any(User.class))).thenReturn(savedUser);

        User result = userService.createUser(incomingVerifiedCitizen, createSampleRequestInfo());

        assertNotNull(result);
        assertEquals(303L, result.getId());
        assertEquals("new-verified-uuid", result.getUuid());
        assertTrue(result.getIsIdentityVerified());
        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("Citizen creation: Rejected when incoming verified citizen has duplicate Aadhaar number on same mobile")
    public void testCreateCitizen_ThrowsExceptionWhenIncomingCitizenHasDuplicateAadhaarOnSameMobile() {
        String mobile = "8588070881";

        User existingVerifiedCitizen = User.builder()
                .id(101L)
                .uuid("existing-verified-uuid")
                .username(mobile)
                .mobileNumber(mobile)
                .name("Existing Verified")
                .aadhaarNumber("XXXXXXXX2222")
                .type(UserType.CITIZEN)
                .isIdentityVerified(true)
                .active(true)
                .tenantId("pg")
                .build();

        when(userRepository.findAll(argThat(criteria -> criteria != null && UserType.CITIZEN.equals(criteria.getType()))))
                .thenReturn(Collections.singletonList(existingVerifiedCitizen));

        User incomingDuplicateAadhaar = User.builder()
                .username("new_with_same_aadhaar")
                .mobileNumber(mobile)
                .name("Duplicate Member")
                .aadhaarNumber("XXXXXXXX2222")
                .isIdentityVerified(true)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        CustomException exception = assertThrows(CustomException.class, () ->
                userService.createUser(incomingDuplicateAadhaar, createSampleRequestInfo())
        );

        assertEquals(UserConstants.ERROR_USER_ALREADY_EXISTS, exception.getCode());
        verify(userRepository, never()).create(any(User.class));
    }

    @Test
    @DisplayName("Citizen creation: Rejected when incoming citizen has duplicate Aadhaar linked to another citizen on a different mobile")
    void testCreateCitizen_ThrowsExceptionWhenIncomingCitizenHasDuplicateAadhaarAcrossDifferentMobile() {
        String existingMobile = "9876543210";
        String incomingMobile = "9123456789";
        String sharedAadhaar = "XXXXXXXX9876";

        User existingCitizenOnOtherMobile = User.builder()
                .id(102L)
                .uuid("other-mobile-user-uuid")
                .username(existingMobile)
                .mobileNumber(existingMobile)
                .name("Citizen Other Mobile")
                .aadhaarNumber(sharedAadhaar)
                .type(UserType.CITIZEN)
                .isIdentityVerified(true)
                .active(true)
                .tenantId("pg")
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenAnswer(invocation -> {
            UserSearchCriteria criteria = invocation.getArgument(0);
            if (sharedAadhaar.equalsIgnoreCase(criteria.getAadhaarNumber())) {
                return Collections.singletonList(existingCitizenOnOtherMobile);
            }
            return Collections.emptyList();
        });

        User incomingCitizen = User.builder()
                .username("new_applicant")
                .mobileNumber(incomingMobile)
                .name("New Applicant")
                .aadhaarNumber(sharedAadhaar)
                .isIdentityVerified(true)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        RequestInfo requestInfo = createSampleRequestInfo();
        CustomException exception = assertThrows(CustomException.class, () ->
                userService.createUser(incomingCitizen, requestInfo)
        );

        assertEquals(UserConstants.ERROR_USER_ALREADY_EXISTS, exception.getCode());
        assertEquals(UserConstants.MSG_USER_ALREADY_EXISTS_AADHAAR, exception.getMessage());
        verify(userRepository, never()).create(any(User.class));
    }

    @Test
    @DisplayName("createUser: When no user exists with same mobile, proceed with standard creation")
    public void testCreateUserWhenMobileDoesNotExist() {
        String mobile = "9999999999";

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());
        when(userRepository.isUserPresent(any(), any(), any())).thenReturn(false);

        User createdMock = User.builder()
                .id(9500L)
                .uuid("new-user-uuid")
                .username("9999999999")
                .mobileNumber(mobile)
                .name("Brand New User")
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        when(userRepository.create(any(User.class))).thenReturn(createdMock);

        User incomingUser = User.builder()
                .username("9999999999")
                .mobileNumber(mobile)
                .name("Brand New User")
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .otpValidationMandatory(false)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        RequestInfo requestInfo = createSampleRequestInfo();

        User result = userService.createUser(incomingUser, requestInfo);

        verify(userRepository, times(1)).create(any(User.class));
        assertEquals("new-user-uuid", result.getUuid());
    }

    @Test
    @DisplayName("createUserWithAddressV2: When mobile has no conflicts, creates new user with address")
    public void testCreateUserWithAddressV2_Success() {
        String mobile = "9999999999";

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());
        when(userRepository.isUserPresent(any(), any(), any())).thenReturn(false);

        Address newAddress = Address.builder()
                .city("City A")
                .pinCode("143001")
                .address("123 Street")
                .type(AddressType.PERMANENT)
                .tenantId("pg.citya")
                .build();

        User incomingUser = User.builder()
                .username("auto_gen_username")
                .mobileNumber(mobile)
                .name("Rohit Verma")
                .emailId("rohit@example.com")
                .addresses(Collections.singleton(newAddress))
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .otpValidationMandatory(false)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        User createdMock = incomingUser.toBuilder().id(9600L).uuid("new-v2-uuid").build();
        when(userRepository.createWithAddressV2(any(User.class))).thenReturn(createdMock);

        RequestInfo requestInfo = createSampleRequestInfo();

        User result = userService.createUserWithAddressV2(incomingUser, requestInfo);

        verify(userRepository, times(1)).createWithAddressV2(any(User.class));
        assertEquals("new-v2-uuid", result.getUuid());
    }

    @Test
    @DisplayName("createUser: When username is duplicate, throws DuplicateUserNameException")
    public void testCreateUser_DuplicateUserNameThrowsException() {
        String mobile = "9999999999";

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());
        when(userRepository.isUserPresent(eq("duplicate_username"), any(), any())).thenReturn(true);

        User incomingUser = User.builder()
                .username("duplicate_username")
                .mobileNumber(mobile)
                .name("Test Citizen")
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .otpValidationMandatory(false)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        assertThrows(DuplicateUserNameException.class, () ->
                userService.createUser(incomingUser, createSampleRequestInfo())
        );

        verify(userRepository, never()).create(any(User.class));
    }
}
