package org.egov.user.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.exception.DuplicateUserNameException;
import org.egov.user.domain.exception.InvalidUpdatePasswordRequestException;
import org.egov.user.domain.exception.InvalidUserCreateException;
import org.egov.user.domain.exception.InvalidUserSearchCriteriaException;
import org.egov.user.domain.exception.OtpValidationPendingException;
import org.egov.user.domain.exception.UserNameNotValidException;
import org.egov.user.domain.exception.UserNotFoundException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.LoggedInUserUpdatePasswordRequest;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, otpRepository, fileRepository, passwordEncoder,
                encryptionDecryptionUtil, 90, true, false, PWD_REGEX, 32, 8, addressRepository);
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
        verify(userRepository).findAll(captured.capture());
        assertThat(captured.getValue().getTenantId()).isEqualTo("pb");
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
        ReflectionTestUtils.setField(userService, "createUserValidateName", true);
        User user = validNewUser();
        user.setName(null);

        assertThatThrownBy(() -> userService.createUser(user, requestWithUser()))
                .isInstanceOf(InvalidUserCreateException.class);
        verifyNoInteractions(userRepository);
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

    @Test
    void isAccountUnlockAble_notLocked_returnsTrue() {
        User user = User.builder().accountLocked(false).build();

        assertThat(userService.isAccountUnlockAble(user)).isTrue();
    }

    @Test
    void isAccountUnlockAble_lockInsideCooldown_returnsFalse() {
        ReflectionTestUtils.setField(userService, "accountUnlockCoolDownPeriod", 30L);
        User user = User.builder()
                .accountLocked(true)
                .accountLockedDate(System.currentTimeMillis())
                .build();

        assertThat(userService.isAccountUnlockAble(user)).isFalse();
    }

    @Test
    void isAccountUnlockAble_lockOlderThanCooldown_returnsTrue() {
        ReflectionTestUtils.setField(userService, "accountUnlockCoolDownPeriod", 1L);
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
        ReflectionTestUtils.setField(userService, "maxInvalidLoginAttempts", 3L);
        ReflectionTestUtils.setField(userService, "maxInvalidLoginAttemptsPeriod", 30L);
        when(userRepository.fetchFailedAttemptsByUserAndTime(eq("uuid-1"), anyLong()))
                .thenReturn(Collections.emptyList());

        userService.handleFailedLogin(storedUser("alice", UserType.CITIZEN), "127.0.0.1", requestWithUser());

        verify(userRepository).insertFailedLoginAttempt(any(FailedLoginAttempt.class));
    }

    @Test
    void handleFailedLogin_attemptsAtThreshold_throwsBadCredentialsException() {
        ReflectionTestUtils.setField(userService, "maxInvalidLoginAttempts", 1L);
        ReflectionTestUtils.setField(userService, "maxInvalidLoginAttemptsPeriod", 30L);
        ReflectionTestUtils.setField(userService, "accountUnlockCoolDownPeriod", 15L);
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
        ReflectionTestUtils.setField(userService, "addressMandatoryFieldsEnabled", true);
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
