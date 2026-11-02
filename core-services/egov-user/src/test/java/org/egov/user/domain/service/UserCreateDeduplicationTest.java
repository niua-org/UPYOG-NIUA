package org.egov.user.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.web.contract.CreateUserRequest;
import org.egov.user.web.contract.CreateUserRequestV2;
import org.egov.user.web.contract.UserDetailResponse;
import org.egov.user.web.contract.UserDetailResponseV2;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.contract.UserRequestV2;
import org.egov.user.web.controller.UserController;
import org.springframework.http.HttpHeaders;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FileStoreRepository;
import org.egov.user.persistence.repository.OtpRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test suite verifying citizen user creation de-duplication and demographic merging logic in {@link UserService}.
 * <p>
 * Ensures that when a citizen registration request arrives for a mobile number already existing in the state tenant,
 * the existing record is updated with null/empty fields and returned rather than creating a duplicate account.
 * <p>
 * Key test scenarios covered:
 * <ul>
 *   <li>Merging null/empty fields for existing citizen on {@code /_create} and {@code /users/v2/_create}</li>
 *   <li>Protection against merging when user type is {@link UserType#EMPLOYEE} (must never merge)</li>
 *   <li>Deactivated existing user ({@code active = false}) is not merged or silently reactivated</li>
 *   <li>Blank-vs-filled Aadhaar and PAN merging protection (unverified create requests cannot bind/overwrite Aadhaar/PAN)</li>
 *   <li>Lookup failure fallback (database query error gracefully falls back to normal user creation)</li>
 *   <li>Multiple matching records (ambiguous matches log a warning and fall back to user creation)</li>
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
    @DisplayName("createUser: When citizen with same mobile number already exists, update null/empty fields and return existing user without creating a new user")
    public void testCreateUserWithExistingMobileUpdatesEmptyFields() {
        String mobile = "8588070881";
        String existingUuid = "existing-uuid-123";

        // Existing user in DB with null name, fatherName, emailId, gender
        User existingUser = User.builder()
                .id(9154L)
                .uuid(existingUuid)
                .username("8588070881")
                .mobileNumber(mobile)
                .name("Citizen")
                .emailId(null)
                .fatherName(null)
                .motherName(null)
                .gender(null)
                .dob(null)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingUser));

        // Incoming user creation request from downstream service
        User incomingUser = User.builder()
                .username("random_uuid_username")
                .mobileNumber(mobile)
                .name("Aarav Sharma")
                .fatherName("Rajesh Sharma")
                .motherName("Sunita Sharma")
                .emailId("aarav@example.com")
                .gender(Gender.MALE)
                .dob(new Date(711072000000L))
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        RequestInfo requestInfo = createSampleRequestInfo();

        User result = userService.createUser(incomingUser, requestInfo);

        // Verify that userRepository.create(...) or persistNewUser was NEVER called
        verify(userRepository, never()).create(any(User.class));
        verify(userRepository, never()).createWithAddressV2(any(User.class));

        // Verify userRepository.update(...) was called to update null/empty fields
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).update(userCaptor.capture(), eq(existingUser), eq(999L), eq("updater-uuid"));

        User capturedUser = userCaptor.getValue();
        // Preserved identity
        assertEquals(existingUuid, capturedUser.getUuid());
        assertEquals(9154L, capturedUser.getId());
        assertEquals("8588070881", capturedUser.getUsername());

        // Updated fields from incoming
        assertEquals("Aarav Sharma", capturedUser.getName());
        assertEquals("Rajesh Sharma", capturedUser.getFatherName());
        assertEquals("Sunita Sharma", capturedUser.getMotherName());
        assertEquals("aarav@example.com", capturedUser.getEmailId());
        assertEquals(Gender.MALE, capturedUser.getGender());
        assertNotNull(capturedUser.getDob());

        // Verify returned user is the existing user
        assertEquals(existingUuid, result.getUuid());
        assertEquals(9154L, result.getId());
        assertEquals("8588070881", result.getUsername());
    }

    @Test
    @DisplayName("createUser: Existing populated fields must NOT be overwritten by incoming request")
    public void testExistingPopulatedFieldsNotOverwritten() {
        String mobile = "8588070881";
        String existingUuid = "existing-uuid-456";

        User existingUser = User.builder()
                .id(9156L)
                .uuid(existingUuid)
                .username("8588070881")
                .mobileNumber(mobile)
                .name("Arpit Rao")
                .fatherName("Ashok Rao")
                .emailId("arpit.existing@example.com")
                .gender(Gender.MALE)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingUser));

        // Incoming user with different values
        User incomingUser = User.builder()
                .username("different_username")
                .mobileNumber(mobile)
                .name("Different Name")
                .fatherName("Different Father")
                .emailId("new.email@example.com")
                .motherName("Rita Rao") // motherName was null in existing
                .gender(Gender.FEMALE)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        RequestInfo requestInfo = createSampleRequestInfo();

        User result = userService.createUser(incomingUser, requestInfo);

        verify(userRepository, never()).create(any(User.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).update(userCaptor.capture(), eq(existingUser), eq(999L), eq("updater-uuid"));

        User capturedUser = userCaptor.getValue();
        // Existing populated fields remained untouched
        assertEquals("Arpit Rao", capturedUser.getName());
        assertEquals("Ashok Rao", capturedUser.getFatherName());
        assertEquals("arpit.existing@example.com", capturedUser.getEmailId());
        assertEquals(Gender.MALE, capturedUser.getGender());

        // Null field was populated from incoming
        assertEquals("Rita Rao", capturedUser.getMotherName());

        assertEquals(existingUuid, result.getUuid());
        assertEquals("8588070881", result.getUsername());
    }

    @Test
    @DisplayName("createUserWithAddressV2: When user exists, update empty fields and enrich addresses via v2 update")
    public void testCreateUserWithAddressV2ExistingMobile() {
        String mobile = "8588070881";
        String existingUuid = "existing-v2-uuid";

        User existingUser = User.builder()
                .id(9200L)
                .uuid(existingUuid)
                .username("8588070881")
                .mobileNumber(mobile)
                .name("Citizen")
                .emailId(null)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingUser));

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
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        RequestInfo requestInfo = createSampleRequestInfo();

        User result = userService.createUserWithAddressV2(incomingUser, requestInfo);

        verify(userRepository, never()).createWithAddressV2(any(User.class));
        verify(userRepository, times(1)).updateV2(any(User.class), eq(existingUser), eq(999L), eq("updater-uuid"));
        verify(addressRepository, never()).createAddressV2(any(Address.class), anyLong(), any());
        verify(addressRepository, never()).create(any(), anyLong(), any());

        assertEquals(existingUuid, result.getUuid());
        assertEquals("8588070881", result.getUsername());
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
        verify(userRepository, never()).update(any(User.class), any(), anyLong(), anyString());
        assertEquals("new-user-uuid", result.getUuid());
    }

    @Test
    @DisplayName("E2E: UserController POST /users/_createnovalidate with existing mobile returns updated existing user")
    public void testE2EUserControllerCreateUserWithoutValidation() {
        UserController userController = new UserController(userService, null);
        String mobile = "8588070881";
        String existingUuid = "e2e-existing-uuid-9154";

        User existingDbUser = User.builder()
                .id(9154L)
                .uuid(existingUuid)
                .username("8588070881")
                .mobileNumber(mobile)
                .name("Citizen")
                .emailId(null)
                .fatherName(null)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));

        UserRequest incomingContractUser = UserRequest.builder()
                .userName("downstream_service_temp_user")
                .mobileNumber(mobile)
                .name("Aarav Sharma")
                .fatherName("Rajesh Sharma")
                .emailId("aarav@example.com")
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(org.egov.user.web.contract.RoleRequest.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        CreateUserRequest request = new CreateUserRequest(createSampleRequestInfo(), incomingContractUser, null);

        UserDetailResponse response = userController.createUserWithoutValidation(request, new HttpHeaders());

        assertNotNull(response);
        assertNotNull(response.getUser());
        assertEquals(1, response.getUser().size());

        UserRequest responseUser = response.getUser().get(0);
        assertEquals(existingUuid, responseUser.getUuid());
        assertEquals(9154L, responseUser.getId());
        assertEquals("8588070881", responseUser.getUserName());
        assertEquals("Aarav Sharma", responseUser.getName());
        assertEquals("Rajesh Sharma", responseUser.getFatherName());
        assertEquals("aarav@example.com", responseUser.getEmailId());

        verify(userRepository, never()).create(any(User.class));
        verify(userRepository, times(1)).update(any(User.class), eq(existingDbUser), anyLong(), anyString());
    }

    @Test
    @DisplayName("E2E: UserController POST /users/v2/_create with existing mobile returns updated existing user")
    public void testE2EUserControllerCreateUserWithAddressV2() {
        UserController userController = new UserController(userService, null);
        String mobile = "8588070881";
        String existingUuid = "e2e-v2-uuid-9156";

        User existingDbUser = User.builder()
                .id(9156L)
                .uuid(existingUuid)
                .username("8588070881")
                .mobileNumber(mobile)
                .name("Arpit Rao")
                .fatherName(null)
                .emailId(null)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));

        UserRequestV2 incomingContractUser = UserRequestV2.builder()
                .userName("downstream_service_v2_user")
                .mobileNumber(mobile)
                .name("Arpit Rao")
                .fatherName("Ashok Rao")
                .emailId("arpit@example.com")
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(org.egov.user.web.contract.RoleRequest.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        CreateUserRequestV2 request = new CreateUserRequestV2(createSampleRequestInfo(), incomingContractUser);

        Object rawResponse = userController.createUserWithAddress(request, new HttpHeaders());

        assertNotNull(rawResponse);
        assertTrue(rawResponse instanceof UserDetailResponseV2);

        UserDetailResponseV2 response = (UserDetailResponseV2) rawResponse;
        assertNotNull(response.getUser());
        assertEquals(1, response.getUser().size());

        UserRequestV2 responseUser = response.getUser().get(0);
        assertEquals(existingUuid, responseUser.getUuid());
        assertEquals(9156L, responseUser.getId());
        assertEquals("8588070881", responseUser.getUserName());
        assertEquals("Arpit Rao", responseUser.getName());
        assertEquals("Ashok Rao", responseUser.getFatherName());
        assertEquals("arpit@example.com", responseUser.getEmailId());

        verify(userRepository, never()).createWithAddressV2(any(User.class));
        verify(userRepository, times(1)).updateV2(any(User.class), eq(existingDbUser), anyLong(), anyString());
    }

    @Test
    @DisplayName("createUser: EMPLOYEE type should not merge with existing citizen on same mobile")
    public void testCreateUserEmployeeTypeDoesNotMerge() {
        String mobile = "8588070881";
        User existingDbCitizen = User.builder()
                .id(101L)
                .uuid("existing-citizen-uuid")
                .mobileNumber(mobile)
                .username(mobile)
                .name("Citizen User")
                .type(UserType.CITIZEN)
                .active(true)
                .tenantId("pg")
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbCitizen));

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

        // Verify update was NEVER called on existing citizen, and create WAS called for new employee
        verify(userRepository, never()).update(any(User.class), eq(existingDbCitizen), anyLong(), anyString());
        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("createUser: Deactivated existing user (active=false) should not be merged or silently reactivated")
    public void testCreateUserDeactivatedExistingUserNotMerged() {
        String mobile = "8588070881";

        // Criteria searches for active=true, so findAll returns empty when only an inactive user exists
        when(userRepository.findAll(argThat(criteria -> criteria != null && Boolean.TRUE.equals(criteria.getActive()))))
                .thenReturn(Collections.emptyList());

        User incomingCitizen = User.builder()
                .username("8588070881")
                .mobileNumber(mobile)
                .name("Active Citizen")
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        User savedCitizen = incomingCitizen.toBuilder().id(303L).uuid("new-active-uuid").build();
        when(userRepository.create(any(User.class))).thenReturn(savedCitizen);

        User result = userService.createUser(incomingCitizen, createSampleRequestInfo());

        assertNotNull(result);
        assertEquals(303L, result.getId());
        assertEquals("new-active-uuid", result.getUuid());

        // Verify update was NEVER called and create was invoked
        verify(userRepository, never()).update(any(User.class), any(User.class), anyLong(), anyString());
        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("createUser: Blank-vs-filled Aadhaar and PAN must NOT be merged to prevent unauthorized binding")
    public void testCreateUserBlankVsFilledAadhaarPanNotMerged() {
        String mobile = "8588070881";
        String existingUuid = "existing-uuid-secure";

        User existingDbUser = User.builder()
                .id(404L)
                .uuid(existingUuid)
                .username(mobile)
                .mobileNumber(mobile)
                .name("Secure Citizen")
                .fatherName(null)
                .emailId(null)
                .pan(null)
                .aadhaarNumber(null)
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));

        User incomingUser = User.builder()
                .username("unverified_req_user")
                .mobileNumber(mobile)
                .name("Secure Citizen")
                .fatherName("Father Citizen")
                .emailId("secure@example.com")
                .pan("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        User result = userService.createUser(incomingUser, createSampleRequestInfo());

        assertNotNull(result);
        assertEquals(404L, result.getId());
        assertEquals(existingUuid, result.getUuid());
        assertEquals("Father Citizen", result.getFatherName());
        assertEquals("secure@example.com", result.getEmailId());

        // Crucial security check: PAN and Aadhaar MUST remain null on existing record
        assertNull(result.getPan(), "PAN must not be merged from unverified create request");
        assertNull(result.getAadhaarNumber(), "Aadhaar must not be merged from unverified create request");

        ArgumentCaptor<User> updatedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).update(updatedUserCaptor.capture(), eq(existingDbUser), anyLong(), anyString());
        assertNull(updatedUserCaptor.getValue().getPan());
        assertNull(updatedUserCaptor.getValue().getAadhaarNumber());
    }

    @Test
    @DisplayName("createUser: Database lookup failure in de-dup gracefully falls back to normal user creation")
    public void testCreateUserLookupFailureFallsBackToCreate() {
        String mobile = "8588070881";

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenThrow(new RuntimeException("Database timeout on search"));

        User incomingCitizen = User.builder()
                .username("8588070881")
                .mobileNumber(mobile)
                .name("Fallback Citizen")
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        User savedCitizen = incomingCitizen.toBuilder().id(505L).uuid("fallback-uuid").build();
        when(userRepository.create(any(User.class))).thenReturn(savedCitizen);

        User result = userService.createUser(incomingCitizen, createSampleRequestInfo());

        assertNotNull(result);
        assertEquals(505L, result.getId());
        assertEquals("fallback-uuid", result.getUuid());

        verify(userRepository, times(1)).create(any(User.class));
        verify(userRepository, never()).update(any(User.class), any(User.class), anyLong(), anyString());
    }

    @Test
    @DisplayName("createUser: Multiple matching records prioritizes the account whose username equals mobileNumber")
    public void testCreateUserMultipleMatchesPrioritizesMatchingUsername() {
        String mobile = "8588070881";

        User matchWithoutMobileAsUser = User.builder()
                .id(11L)
                .uuid("uuid-11")
                .mobileNumber(mobile)
                .username("custom_user_login")
                .name("Legacy Citizen")
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .build();

        User matchWithMobileAsUser = User.builder()
                .id(22L)
                .uuid("uuid-22")
                .mobileNumber(mobile)
                .username(mobile)
                .name("Standard Citizen")
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .active(true)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenAnswer(invocation -> {
            UserSearchCriteria criteria = invocation.getArgument(0);
            if (criteria.getUuid() != null && criteria.getUuid().contains("uuid-22")) {
                return Collections.singletonList(matchWithMobileAsUser);
            }
            if (criteria.getUuid() != null && criteria.getUuid().contains("uuid-11")) {
                return Collections.singletonList(matchWithoutMobileAsUser);
            }
            return Arrays.asList(matchWithoutMobileAsUser, matchWithMobileAsUser);
        });

        User incomingCitizen = User.builder()
                .username("8588070881")
                .mobileNumber(mobile)
                .name("Standard Citizen")
                .emailId("standard@example.com")
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.builder().code("CITIZEN").tenantId("pg").build()))
                .build();

        User result = userService.createUser(incomingCitizen, createSampleRequestInfo());

        assertNotNull(result);
        // Must select matchWithMobileAsUser (id 22) because its username exactly matches the mobile number
        assertEquals(22L, result.getId());
        assertEquals("uuid-22", result.getUuid());

        verify(userRepository, times(1)).update(any(User.class), eq(matchWithMobileAsUser), anyLong(), anyString());
        verify(userRepository, never()).create(any(User.class));
    }
}
