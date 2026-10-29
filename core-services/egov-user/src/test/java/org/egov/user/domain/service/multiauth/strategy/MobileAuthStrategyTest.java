package org.egov.user.domain.service.multiauth.strategy;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.UserService;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.OtpRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.OtpValidateRequest;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MobileAuthStrategy} verifying single user login,
 * multi-user account detection for shared mobile numbers, and selected profile resolution.
 */
@ExtendWith(MockitoExtension.class)
public class MobileAuthStrategyTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    private MobileAuthStrategy mobileAuthStrategy;

    @BeforeEach
    public void setUp() {
        org.egov.user.config.UserConfiguration config = new org.egov.user.config.UserConfiguration();
        config.setCitizenLoginPasswordOtpFixedEnabled(false);
        config.setRolesStateLevelEnabled(true);
        config.setOtpMaxInvalidAttempts(3);
        mobileAuthStrategy = new MobileAuthStrategy(userService, userRepository, otpRepository, encryptionDecryptionUtil, config);
    }

    private User buildUser(Long id, String uuid, String username, String mobile, String name, String aadhaar) {
        return User.builder()
                .id(id)
                .uuid(uuid)
                .username(username)
                .mobileNumber(mobile)
                .name(name)
                .aadhaarNumber(aadhaar)
                .gender(Gender.MALE)
                .dob(new Date())
                .type(UserType.CITIZEN)
                .tenantId("pb")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should return single authenticated user when exactly 1 account matches mobile number")
    public void testAuthenticate_WhenSingleUserFound_ReturnsSingleUser() {
        LoginV2Request request = LoginV2Request.builder()
                .mobileNumber("9876543210")
                .otp("123456")
                .tenantId("pb.amritsar")
                .requestInfo(new RequestInfo())
                .build();

        User existingUser = buildUser(101L, "uuid-user-1", "9876543210", "9876543210", "Ravi Kumar", "XXXXXXXX1234");

        when(otpRepository.validateOtp(any(OtpValidateRequest.class))).thenReturn(true);
        when(encryptionDecryptionUtil.encryptObject(any(UserSearchCriteria.class), eq(UserConstants.USER_ENCRYPTION_KEY), eq(UserSearchCriteria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingUser));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), eq(User.class), any()))
                .thenReturn(Collections.singletonList(existingUser));

        AuthenticatedUserResult result = mobileAuthStrategy.authenticateAndResolveUser(request);

        assertNotNull(result);
        assertFalse(result.isMultipleUsersFound());
        assertNotNull(result.getUser());
        assertEquals("uuid-user-1", result.getUser().getUuid());
        assertEquals(AuthType.MOBILE_NUMBER, result.getAuthType());
        assertFalse(result.isNewUser());
    }

    @Test
    @DisplayName("Should flag multipleUsersFound and return list when 2 or more accounts share the same mobile")
    public void testAuthenticate_WhenMultipleUsersFound_ReturnsMultipleUsersResult() {
        LoginV2Request request = LoginV2Request.builder()
                .mobileNumber("9876543210")
                .otp("123456")
                .tenantId("pb.amritsar")
                .requestInfo(new RequestInfo())
                .build();

        User father = buildUser(101L, "uuid-father", "9876543210", "9876543210", "Ravi Kumar (Father)", "XXXXXXXX1111");
        User son = buildUser(102L, "uuid-son", "9876543210_sub1", "9876543210", "Aarav Kumar (Son)", "XXXXXXXX2222");
        List<User> userList = Arrays.asList(father, son);

        when(otpRepository.validateOtp(any(OtpValidateRequest.class))).thenReturn(true);
        when(encryptionDecryptionUtil.encryptObject(any(UserSearchCriteria.class), eq(UserConstants.USER_ENCRYPTION_KEY), eq(UserSearchCriteria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(userList);
        when(encryptionDecryptionUtil.decryptObject(any(), any(), eq(User.class), any()))
                .thenReturn(userList);

        AuthenticatedUserResult result = mobileAuthStrategy.authenticateAndResolveUser(request);

        assertNotNull(result);
        assertTrue(result.isMultipleUsersFound());
        assertNull(result.getUser());
        assertNotNull(result.getMatchedUsers());
        assertEquals(2, result.getMatchedUsers().size());
        assertEquals("uuid-father", result.getMatchedUsers().get(0).getUuid());
        assertEquals("uuid-son", result.getMatchedUsers().get(1).getUuid());
    }

    @Test
    @DisplayName("Should authenticate selected profile when selectedUserUuid is explicitly provided")
    public void testAuthenticate_WhenSelectedUserUuidProvided_ResolvesSpecificUser() {
        LoginV2Request request = LoginV2Request.builder()
                .mobileNumber("9876543210")
                .otp("123456")
                .tenantId("pb.amritsar")
                .selectedUserUuid("uuid-son")
                .requestInfo(new RequestInfo())
                .build();

        User son = buildUser(102L, "uuid-son", "9876543210_sub1", "9876543210", "Aarav Kumar (Son)", "XXXXXXXX2222");

        when(otpRepository.validateOtp(any(OtpValidateRequest.class))).thenReturn(true);
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(son));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), eq(User.class), any()))
                .thenReturn(Collections.singletonList(son));

        AuthenticatedUserResult result = mobileAuthStrategy.authenticateAndResolveUser(request);

        assertNotNull(result);
        assertFalse(result.isMultipleUsersFound());
        assertNotNull(result.getUser());
        assertEquals("uuid-son", result.getUser().getUuid());
        assertEquals("Aarav Kumar (Son)", result.getUser().getName());
        assertEquals(AuthType.MOBILE_NUMBER, result.getAuthType());
    }

    @Test
    @DisplayName("Should auto-provision new citizen profile when no account exists for mobile")
    public void testAuthenticate_WhenNoAccountExists_AutoProvisionsNewUser() {
        LoginV2Request request = LoginV2Request.builder()
                .mobileNumber("9876543210")
                .otp("123456")
                .name("New Citizen")
                .tenantId("pb.amritsar")
                .requestInfo(new RequestInfo())
                .build();

        User newlyCreated = buildUser(201L, "uuid-new-citizen", "9876543210", "9876543210", "New Citizen", null);

        when(otpRepository.validateOtp(any(OtpValidateRequest.class))).thenReturn(true);
        when(encryptionDecryptionUtil.encryptObject(any(UserSearchCriteria.class), eq(UserConstants.USER_ENCRYPTION_KEY), eq(UserSearchCriteria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());
        when(userRepository.isUserPresent(eq("9876543210"), eq("pb"), eq(UserType.CITIZEN))).thenReturn(false);
        when(userService.createUser(any(User.class), any(RequestInfo.class))).thenReturn(newlyCreated);

        AuthenticatedUserResult result = mobileAuthStrategy.authenticateAndResolveUser(request);

        assertNotNull(result);
        assertFalse(result.isMultipleUsersFound());
        assertTrue(result.isNewUser());
        assertNotNull(result.getUser());
        assertEquals("uuid-new-citizen", result.getUser().getUuid());
        verify(userService, times(1)).createUser(any(User.class), any(RequestInfo.class));
    }
}
