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
}
