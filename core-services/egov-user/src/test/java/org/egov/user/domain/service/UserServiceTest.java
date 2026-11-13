package org.egov.user.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.exception.DuplicateUserNameException;
import org.egov.user.domain.exception.InvalidUpdatePasswordRequestException;
import org.egov.user.domain.exception.InvalidUserCreateException;
import org.egov.user.domain.exception.InvalidUserSearchCriteriaException;
import org.egov.user.domain.exception.OtpValidationPendingException;
import org.egov.user.domain.exception.PasswordMismatchException;
import org.egov.user.domain.exception.UserNameNotValidException;
import org.egov.user.domain.exception.UserNotFoundException;
import org.egov.user.domain.exception.UserProfileUpdateDeniedException;
import org.egov.user.domain.model.NonLoggedInUserUpdatePasswordRequest;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.LoggedInUserUpdatePasswordRequest;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.NotificationUtil;
import org.egov.user.persistence.dto.FailedLoginAttempt;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FileStoreRepository;
import org.egov.user.persistence.repository.OtpRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String PWD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).*$";

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
    @Mock
    private NotificationUtil notificationUtil;

    private UserService userService;
    private UserConfiguration userConfiguration;

    @BeforeEach
    void setUp() {
        userConfiguration = UserConfiguration.builder()
                .defaultPasswordExpiryInDays(90)
                .citizenLoginPasswordOtpEnabled(true)
                .employeeLoginPasswordOtpEnabled(false)
                .pwdRegex(PWD_REGEX)
                .pwdMaxLength(32)
                .pwdMinLength(8)
                .accountUnlockCoolDownPeriod(30L)
                .maxInvalidLoginAttemptsPeriod(30L)
                .maxInvalidLoginAttempts(5L)
                .createUserValidateName(true)
                .digilockerSearch(false)
                .citizenLoginPasswordOtpFixedEnabled(false)
                .addressMandatoryFieldsEnabled(true)
                .build();
        userService = new UserService(userRepository, otpRepository, fileRepository, passwordEncoder,
                encryptionDecryptionUtil, addressRepository, userConfiguration);
        ReflectionTestUtils.setField(userService, "notificationUtil", notificationUtil);
        lenient().when(encryptionDecryptionUtil.encryptObject(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), nullable(RequestInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
    }

    @Test
    void getUniqueUser_singleCitizenMatch_returnsDecryptedUser() {
        User stored = storedUser("alice", UserType.CITIZEN);
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder().uuid("actor").build())
                .build();

        User result = userService.getUniqueUser("alice", "pb.amritsar", UserType.CITIZEN, requestInfo);

        assertThat(result.getUsername()).isEqualTo("alice");
        ArgumentCaptor<UserSearchCriteria> criteria = ArgumentCaptor.forClass(UserSearchCriteria.class);
        verify(userRepository).findAll(criteria.capture());
        assertThat(criteria.getValue().getTenantId()).isEqualTo("pb");
        verify(encryptionDecryptionUtil).decryptObject(eq(stored), eq("User"), eq(User.class), eq(requestInfo));
    }

    @Test
    void getUniqueUser_blankUserName_throwsUserNotFoundException() {
        assertThatThrownBy(() -> userService.getUniqueUser("", "pb", UserType.CITIZEN, RequestInfo.builder().build()))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).findAll(any());
    }

    @Test
    void getUniqueUser_noMatchingUser_throwsUserNotFoundException() {
        when(userRepository.findAll(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> userService.getUniqueUser("alice", "pb", UserType.EMPLOYEE, requestWithUser()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUniqueUser_duplicateUsers_throwsDuplicateUserNameException() {
        when(userRepository.findAll(any())).thenReturn(Arrays.asList(storedUser("alice", UserType.EMPLOYEE),
                storedUser("alice", UserType.EMPLOYEE)));

        assertThatThrownBy(() -> userService.getUniqueUser("alice", "pb", UserType.EMPLOYEE, requestWithUser()))
                .isInstanceOf(DuplicateUserNameException.class);
    }

    @Test
    void getUniqueUser_authenticationWithoutUserInfo_skipsDecryption() {
        User stored = storedUser("alice", UserType.EMPLOYEE);
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));

        User result = userService.getUniqueUser("alice", "pb", UserType.EMPLOYEE, RequestInfo.builder().build());

        assertThat(result).isSameAs(stored);
        verify(encryptionDecryptionUtil, never()).decryptObject(any(), any(), any(), any());
    }

    @Test
    void getUniqueUser_decryptionFailure_returnsEncryptedUser() {
        User stored = storedUser("alice", UserType.EMPLOYEE);
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), nullable(RequestInfo.class)))
                .thenThrow(new IllegalStateException("bad ciphertext"));

        User result = userService.getUniqueUser("alice", "pb.amritsar", UserType.EMPLOYEE, requestWithUser());

        assertThat(result).isSameAs(stored);
    }

    @Test
    void getUserByUuid_blankUuid_throwsUserNotFoundException() {
        assertThatThrownBy(() -> userService.getUserByUuid(""))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).findAll(any());
    }

    @Test
    void getUserByUuid_knownUuid_returnsUser() {
        User stored = storedUser("alice", UserType.EMPLOYEE);
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));

        assertThat(userService.getUserByUuid("uuid-1").getUsername()).isEqualTo("alice");
    }

    @Test
    void getUserBymobileNumber_blankMobile_throwsUserNotFoundException() {
        assertThatThrownBy(() -> userService.getUserBymobileNumber(null))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).findAll(any());
    }

    @Test
    void searchUsers_noMatches_returnsEmptyList() {
        when(userRepository.findAll(any())).thenReturn(Collections.emptyList());
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .uuid(Collections.singletonList("missing"))
                .build();

        assertThat(userService.searchUsers(criteria, false, RequestInfo.builder().build())).isEmpty();
        verify(encryptionDecryptionUtil, never()).decryptObject(any(), any(), any(), any());
    }

    @Test
    void searchUsers_citizenWithMobile_returnsDecryptedUsersAndStateTenant() {
        User stored = storedUser("alice", UserType.CITIZEN);
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .userName("alice")
                .mobileNumber("9999999999")
                .tenantId("pb.amritsar")
                .type(UserType.CITIZEN)
                .build();

        List<User> result = userService.searchUsers(criteria, false, requestWithUser());

        assertThat(result).containsExactly(stored);
        ArgumentCaptor<UserSearchCriteria> captured = ArgumentCaptor.forClass(UserSearchCriteria.class);
        verify(userRepository, org.mockito.Mockito.atLeastOnce()).findAll(captured.capture());
        assertThat(captured.getAllValues().stream().anyMatch(c -> "pb".equals(c.getTenantId()))).isTrue();
        assertThat(captured.getValue().getAlternatemobilenumber()).isEqualTo("9999999999");
    }

    @Test
    void searchUsers_userNameWithoutTenant_throwsInvalidUserSearchCriteriaException() {
        UserSearchCriteria criteria = UserSearchCriteria.builder().userName("alice").build();

        assertThatThrownBy(() -> userService.searchUsers(criteria, false, RequestInfo.builder().build()))
                .isInstanceOf(InvalidUserSearchCriteriaException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void searchUsers_decryptionFailure_returnsRepositoryList() {
        User stored = storedUser("alice", UserType.EMPLOYEE);
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), nullable(RequestInfo.class)))
                .thenThrow(new IllegalStateException("bad ciphertext"));
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .userName("alice")
                .tenantId("pb")
                .type(UserType.EMPLOYEE)
                .build();

        assertThat(userService.searchUsers(criteria, false, requestWithUser())).containsExactly(stored);
    }

    @Test
    void createUser_validCitizenWithoutPassword_persistsStateTenant() {
        when(userRepository.isUserPresent(anyString(), anyString(), any())).thenReturn(false);
        when(userRepository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(validNewUser(), requestWithUser());

        assertThat(created.getUuid()).isNotBlank();
        assertThat(created.getTenantId()).isEqualTo("pb");
        assertThat(created.getPassword()).isEqualTo("hashed-password");
        verify(userRepository).isUserPresent("alice", "pb", UserType.CITIZEN);
        verify(userRepository).create(created);
    }

    @Test
    void createUser_duplicateUsername_throwsDuplicateUserNameException() {
        when(userRepository.isUserPresent(anyString(), anyString(), any())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validNewUser(), requestWithUser()))
                .isInstanceOf(DuplicateUserNameException.class);
        verify(userRepository, never()).create(any());
    }

    @Test
    void createUser_nameRequiredAndMissing_throwsInvalidUserCreateException() {
        userConfiguration.setCreateUserValidateName(true);
        User user = validNewUser();
        user.setName(null);

        lenient().when(userRepository.findAll(any())).thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> userService.createUser(user, requestWithUser()))
                .isInstanceOf(InvalidUserCreateException.class);
        verify(userRepository, org.mockito.Mockito.never()).create(any());
    }

    @Test
    void createUser_otpRejected_throwsOtpValidationPendingException() {
        User user = validNewUser();
        user.setOtpValidationMandatory(true);
        user.setOtpReference("111111");
        when(otpRepository.validateOtp(any())).thenReturn(false);

        assertThatThrownBy(() -> userService.createUser(user, requestWithUser()))
                .isInstanceOf(OtpValidationPendingException.class);
        verify(userRepository, never()).create(any());
    }

    @Test
    void createCitizen_nonNumericUsernameWhenOtpEnabled_throwsUserNameNotValidException() {
        User user = User.builder().username("alice").build();

        assertThatThrownBy(() -> userService.createCitizen(user, requestWithUser()))
                .isInstanceOf(UserNameNotValidException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void validatePassword_shorterThanMinimum_throwsCustomException() {
        assertThatThrownBy(() -> userService.validatePassword("Aa1@"))
                .isInstanceOfSatisfying(CustomException.class, ex ->
                        assertThat(ex.getErrors()).containsKey("INVALID_PWD_LENGTH"));
    }

    @Test
    void validatePassword_matchingPattern_doesNotThrow() {
        userService.validatePassword("Abcdef1@");
    }

    @Test
    void validatePassword_blankPassword_doesNotThrow() {
        userService.validatePassword("");
        userService.validatePassword(null);
    }

    @Test
    void encryptPwd_nullPassword_returnsNull() {
        assertThat(userService.encryptPwd(null)).isNull();
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void encryptPwd_rawPassword_returnsEncodedValue() {
        assertThat(userService.encryptPwd("Abcdef1@")).isEqualTo("hashed-password");
    }

    @Test
    void decryptUserWithContext_nullUser_returnsNull() {
        assertThat(userService.decryptUserWithContext(null, null)).isNull();
        verifyNoInteractions(encryptionDecryptionUtil);
    }

    /**
     * Fix for PR Comment #20 (UserService.java:459):
     * Verifies that decryptUserWithContext builds RequestInfo with typed User and Role contract objects,
     * preserving null role attributes rather than forcing empty strings.
     */
    @Test
    void decryptUserWithContext_validUserAndAccount_constructsTypedRequestInfo() {
        User encryptedUser = User.builder()
                .id(101L)
                .uuid("uuid-101")
                .username("enc_username")
                .build();

        org.egov.user.web.contract.auth.Role authRole = new org.egov.user.web.contract.auth.Role();
        authRole.setCode("CITIZEN");
        authRole.setName(null); // Explicit null attribute to verify it is NOT converted to ""
        authRole.setTenantId("pg");

        org.egov.user.web.contract.auth.User authAccount = org.egov.user.web.contract.auth.User.builder()
                .id(101L)
                .uuid("uuid-101")
                .type("CITIZEN")
                .tenantId("pg")
                .roles(Collections.singleton(authRole))
                .build();

        ArgumentCaptor<RequestInfo> requestInfoCaptor = ArgumentCaptor.forClass(RequestInfo.class);
        when(encryptionDecryptionUtil.decryptObject(any(), eq("User"), eq(User.class), requestInfoCaptor.capture()))
                .thenReturn(Collections.singletonList(encryptedUser));

        User result = userService.decryptUserWithContext(encryptedUser, authAccount);

        assertThat(result).isNotNull();
        RequestInfo capturedReqInfo = requestInfoCaptor.getValue();
        assertThat(capturedReqInfo).isNotNull();
        assertThat(capturedReqInfo.getUserInfo()).isNotNull();
        assertThat(capturedReqInfo.getUserInfo().getUuid()).isEqualTo("uuid-101");
        assertThat(capturedReqInfo.getUserInfo().getRoles()).hasSize(1);

        org.egov.common.contract.request.Role capturedRole = capturedReqInfo.getUserInfo().getRoles().get(0);
        assertThat(capturedRole.getCode()).isEqualTo("CITIZEN");
        assertThat(capturedRole.getName()).isNull(); // Must remain null, NOT converted to ""
        assertThat(capturedRole.getTenantId()).isEqualTo("pg");
    }

    @Test
    void isAccountUnlockAble_notLocked_returnsTrue() {
        User user = User.builder().accountLocked(false).build();

        assertThat(userService.isAccountUnlockAble(user)).isTrue();
    }

    @Test
    void isAccountUnlockAble_lockInsideCooldown_returnsFalse() {
        userConfiguration.setAccountUnlockCoolDownPeriod(30L);
        User user = User.builder()
                .accountLocked(true)
                .accountLockedDate(System.currentTimeMillis())
                .build();

        assertThat(userService.isAccountUnlockAble(user)).isFalse();
    }

    @Test
    void isAccountUnlockAble_lockOlderThanCooldown_returnsTrue() {
        userConfiguration.setAccountUnlockCoolDownPeriod(1L);
        User user = User.builder()
                .accountLocked(true)
                .accountLockedDate(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5))
                .build();

        assertThat(userService.isAccountUnlockAble(user)).isTrue();
    }

    @Test
    void resetFailedLoginAttempts_nullUser_doesNotTouchRepository() {
        userService.resetFailedLoginAttempts((User) null);

        verify(userRepository, never()).resetFailedLoginAttemptsForUser(any());
    }

    @Test
    void resetFailedLoginAttempts_withUuid_resetsAttempts() {
        userService.resetFailedLoginAttempts(User.builder().uuid("uuid-1").build());

        verify(userRepository).resetFailedLoginAttemptsForUser("uuid-1");
    }

    @Test
    void handleFailedLogin_nullUuid_doesNothing() {
        userService.handleFailedLogin(User.builder().build(), "127.0.0.1", RequestInfo.builder().build());

        verifyNoInteractions(userRepository);
    }

    @Test
    void handleFailedLogin_belowThreshold_recordsAttempt() {
        userConfiguration.setMaxInvalidLoginAttempts(3L);
        userConfiguration.setMaxInvalidLoginAttemptsPeriod(30L);
        when(userRepository.fetchFailedAttemptsByUserAndTime(eq("uuid-1"), anyLong()))
                .thenReturn(Collections.emptyList());

        userService.handleFailedLogin(storedUser("alice", UserType.CITIZEN), "127.0.0.1", requestWithUser());

        verify(userRepository).insertFailedLoginAttempt(any(FailedLoginAttempt.class));
    }

    @Test
    void handleFailedLogin_attemptsAtThreshold_throwsBadCredentialsException() {
        userConfiguration.setMaxInvalidLoginAttempts(1L);
        userConfiguration.setMaxInvalidLoginAttemptsPeriod(30L);
        userConfiguration.setAccountUnlockCoolDownPeriod(15L);
        when(userRepository.fetchFailedAttemptsByUserAndTime(eq("uuid-1"), anyLong()))
                .thenReturn(Collections.emptyList());
        UserService spyService = spy(userService);
        doReturn(storedUser("alice", UserType.CITIZEN)).when(spyService)
                .updateWithoutOtpValidation(any(User.class), any());

        assertThatThrownBy(() -> spyService.handleFailedLogin(storedUser("alice", UserType.CITIZEN), "10.0.0.1",
                requestWithUser()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Account locked");
        verify(userRepository, never()).insertFailedLoginAttempt(any());
    }

    @Test
    void updatePasswordForLoggedInUser_citizenOtpLogin_throwsInvalidUpdatePasswordRequestException() {
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(storedUser("9999999999", UserType.CITIZEN)));
        LoggedInUserUpdatePasswordRequest request = LoggedInUserUpdatePasswordRequest.builder()
                .userName("9999999999")
                .tenantId("pb")
                .type(UserType.CITIZEN)
                .existingPassword("Oldpass1@")
                .newPassword("Newpass1@")
                .build();

        assertThatThrownBy(() -> userService.updatePasswordForLoggedInUser(request))
                .isInstanceOf(InvalidUpdatePasswordRequestException.class);
        verify(userRepository, never()).update(any(User.class), any(User.class), anyLong(), any());
    }

    @Test
    void createAddress_unknownUserUuid_throwsIllegalArgumentException() {
        when(userRepository.getUserIdByUuid("missing")).thenReturn(null);

        assertThatThrownBy(() -> userService.createAddress("missing", Address.builder().city("City").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("USER_UUID_NOT_VALID");
    }

    @Test
    void createAddress_mandatoryFieldsMissing_throwsCustomException() {
        userConfiguration.setAddressMandatoryFieldsEnabled(true);
        when(userRepository.getUserIdByUuid("uuid-1")).thenReturn(5L);

        assertThatThrownBy(() -> userService.createAddress("uuid-1", Address.builder().build()))
                .isInstanceOfSatisfying(CustomException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("ADDRESS_VALIDATION_ERROR"));
        verifyNoInteractions(addressRepository);
    }

    @Test
    void getAddress_noneStored_returnsEmptyList() {
        when(addressRepository.getAddressV2(any())).thenReturn(Collections.emptyList());

        assertThat(userService.getAddress("uuid-1", "pb")).isEmpty();
        verify(encryptionDecryptionUtil, never()).decryptObject(any(), any(), any(), any());
    }

    @Test
    void removeTokensByUser_anyUser_doesNotCallRepository() {
        userService.removeTokensByUser(storedUser("alice", UserType.CITIZEN));

        verifyNoInteractions(userRepository);
    }

    @Test
    void validateOtp_validReference_delegatesToOtpRepository() {
        User user = storedUser("alice", UserType.CITIZEN).toBuilder()
                .otpReference("654321")
                .build();
        when(otpRepository.validateOtp(any())).thenReturn(true);

        assertThat(userService.validateOtp(user)).isTrue();
        verify(otpRepository).validateOtp(any());
    }

    @Test
    void validateOtp_invalidOtp_returnsFalse() {
        User user = storedUser("alice", UserType.CITIZEN).toBuilder()
                .otpReference("000000")
                .build();
        when(otpRepository.validateOtp(any())).thenReturn(false);

        assertThat(userService.validateOtp(user)).isFalse();
    }

    @Test
    void updatePasswordForLoggedInUser_validEmployee_updatesRepository() {
        userConfiguration.setCitizenLoginPasswordOtpEnabled(false);
        User stored = storedUser("alice", UserType.EMPLOYEE).toBuilder().password("existing-hash").build();
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));
        when(passwordEncoder.matches("Oldpass1@", "existing-hash")).thenReturn(true);
        LoggedInUserUpdatePasswordRequest request = LoggedInUserUpdatePasswordRequest.builder()
                .userName("alice")
                .tenantId("pb")
                .type(UserType.EMPLOYEE)
                .existingPassword("Oldpass1@")
                .newPassword("Newpass1@")
                .build();

        userService.updatePasswordForLoggedInUser(request);

        verify(userRepository).update(any(User.class), eq(stored), eq(1L), eq("uuid-1"));
    }

    @Test
    void updatePasswordForLoggedInUser_wrongExistingPassword_throwsPasswordMismatchException() {
        userConfiguration.setCitizenLoginPasswordOtpEnabled(false);
        User stored = storedUser("alice", UserType.EMPLOYEE).toBuilder().password("existing-hash").build();
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));
        when(passwordEncoder.matches(anyString(), eq("existing-hash"))).thenReturn(false);
        LoggedInUserUpdatePasswordRequest request = LoggedInUserUpdatePasswordRequest.builder()
                .userName("alice")
                .tenantId("pb")
                .type(UserType.EMPLOYEE)
                .existingPassword("Wrongpass1@")
                .newPassword("Newpass1@")
                .build();

        assertThatThrownBy(() -> userService.updatePasswordForLoggedInUser(request))
                .isInstanceOf(PasswordMismatchException.class);
        verify(userRepository, never()).update(any(), any(), anyLong(), any());
    }

    @Test
    void updatePasswordForNonLoggedInUser_validOtp_updatesEncryptedUser() {
        userConfiguration.setCitizenLoginPasswordOtpEnabled(false);
        User stored = storedUser("alice", UserType.EMPLOYEE).toBuilder().password("existing-hash").build();
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));
        when(otpRepository.validateOtp(any())).thenReturn(true);
        NonLoggedInUserUpdatePasswordRequest request = NonLoggedInUserUpdatePasswordRequest.builder()
                .userName("alice")
                .tenantId("pb")
                .type(UserType.EMPLOYEE)
                .otpReference("123456")
                .newPassword("Newpass1@")
                .build();

        userService.updatePasswordForNonLoggedInUser(request, requestWithUser());

        verify(encryptionDecryptionUtil, org.mockito.Mockito.atLeastOnce())
                .decryptObject(any(), eq("User"), eq(User.class), any());
        verify(encryptionDecryptionUtil).encryptObject(any(), eq("User"), eq(User.class));
        verify(userRepository).update(any(User.class), any(User.class), eq(1L), eq("actor"));
    }

    @Test
    void partialUpdate_sameLoggedInUser_returnsDecryptedUser() {
        User existing = storedUser("alice", UserType.EMPLOYEE);
        User patch = existing.toBuilder()
                .loggedInUserId(1L)
                .loggedInUserUuid("uuid-1")
                .emailId("new@example.com")
                .build();
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(existing));

        User updated = userService.partialUpdate(patch, requestWithUser());

        assertThat(updated).isSameAs(existing);
        verify(userRepository).update(any(User.class), eq(existing), eq(1L), eq("actor"));
    }

    @Test
    void partialUpdate_differentLoggedInUser_throwsUserProfileUpdateDeniedException() {
        User existing = storedUser("alice", UserType.EMPLOYEE);
        User patch = existing.toBuilder()
                .loggedInUserId(99L)
                .loggedInUserUuid("other-uuid")
                .build();
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(existing));

        assertThatThrownBy(() -> userService.partialUpdate(patch, requestWithUser()))
                .isInstanceOf(UserProfileUpdateDeniedException.class);
        verify(userRepository, never()).update(any(), any(), anyLong(), any());
    }

    @Test
    void updateWithoutOtpValidation_unlocksAccount_resetsFailedAttempts() {
        User locked = storedUser("alice", UserType.EMPLOYEE).toBuilder()
                .accountLocked(true)
                .roles(Set.of(Role.builder().code("EMPLOYEE").build()))
                .build();
        User unlockRequest = locked.toBuilder()
                .accountLocked(false)
                .password("Newpass1@")
                .loggedInUserId(1L)
                .loggedInUserUuid("uuid-1")
                .build();
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(locked));

        userService.updateWithoutOtpValidation(unlockRequest, requestWithUser());

        verify(userRepository).resetFailedLoginAttemptsForUser("uuid-1");
    }

    @Test
    void getUserBymobileNumber_knownMobile_returnsUser() {
        User stored = storedUser("alice", UserType.CITIZEN);
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(stored));

        assertThat(userService.getUserBymobileNumber("9999999999").getUsername()).isEqualTo("alice");
    }

    @Test
    void createAddress_validRequest_persistsAddress() {
        userConfiguration.setAddressMandatoryFieldsEnabled(false);
        when(userRepository.getUserIdByUuid("uuid-1")).thenReturn(5L);
        Address saved = Address.builder().city("City").build();
        when(addressRepository.createAddressV2(any(), eq(5L), any())).thenReturn(saved);

        Address created = userService.createAddress("uuid-1", Address.builder().city("City").tenantId("pb").build());

        assertThat(created.getCity()).isEqualTo("City");
        verify(addressRepository).createAddressV2(any(Address.class), eq(5L), eq("pb"));
    }

    @Test
    void searchUsersV2_withMatches_returnsDecryptedUsers() {
        User stored = storedUser("alice", UserType.CITIZEN);
        when(userRepository.findAllV2(any())).thenReturn(Collections.singletonList(stored));
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .userName("alice")
                .tenantId("pb.amritsar")
                .type(UserType.CITIZEN)
                .build();

        List<User> result = userService.searchUsersV2(criteria, false, requestWithUser());

        assertThat(result).containsExactly(stored);
        verify(userRepository).findAllV2(any(UserSearchCriteria.class));
    }

    @Test
    void getAddress_storedAddresses_returnsDecryptedList() {
        Address stored = Address.builder().city("City").build();
        when(addressRepository.getAddressV2(any())).thenReturn(Collections.singletonList(stored));

        assertThat(userService.getAddress("uuid-1", "pb")).containsExactly(stored);
        verify(encryptionDecryptionUtil).decryptObject(eq(Collections.singletonList(stored)),
                eq(org.egov.user.domain.service.utils.UserConstants.USER_ADDRESS_ENCRYPTION_KEY),
                eq(Address.class), eq(null));
    }

    private RequestInfo requestWithUser() {
        return RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder().uuid("actor").id(1L).build())
                .build();
    }

    private User storedUser(String username, UserType type) {
        return User.builder()
                .id(1L)
                .uuid("uuid-1")
                .username(username)
                .name("Alice")
                .mobileNumber("9999999999")
                .active(true)
                .type(type)
                .tenantId("pb")
                .accountLocked(false)
                .roles(Set.of(Role.builder().code("CITIZEN").build()))
                .build();
    }

    private User validNewUser() {
        return User.builder()
                .username("alice")
                .name("Alice")
                .mobileNumber("9999999999")
                .active(true)
                .type(UserType.CITIZEN)
                .tenantId("pb.amritsar")
                .roles(Set.of(Role.builder().code("CITIZEN").build()))
                .build();
    }
}
