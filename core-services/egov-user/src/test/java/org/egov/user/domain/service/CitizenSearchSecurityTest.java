package org.egov.user.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.profile.validator.ProfileValidatorImpl;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.contract.profile.ProfileSearchRequest;
import org.egov.user.web.contract.profile.ProfileUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CitizenSearchSecurityTest {

    private ProfileValidatorImpl profileValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    public void setUp() {
        profileValidator = new ProfileValidatorImpl(userRepository);

        userService = new UserService(
                userRepository,
                null,
                null,
                passwordEncoder,
                encryptionDecryptionUtil,
                90,
                false,
                false,
                "pg",
                10,
                10,
                null
        );

        when(encryptionDecryptionUtil.encryptObject(any(UserSearchCriteria.class), any(), eq(UserSearchCriteria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionDecryptionUtil.encryptObject(any(User.class), any(), eq(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(User.class), any(), eq(User.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
    }

    private RequestInfo createCitizenRequestInfo(String uuid, String mobile) {
        return RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(101L)
                        .uuid(uuid)
                        .userName(mobile)
                        .mobileNumber(mobile)
                        .type("CITIZEN")
                        .build())
                .build();
    }

    private RequestInfo createEmployeeRequestInfo(String uuid) {
        return RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(501L)
                        .uuid(uuid)
                        .userName("emp501")
                        .type("EMPLOYEE")
                        .build())
                .build();
    }

    // =========================================================================
    // 1. ProfileValidator (Used by /profile/_search)
    // =========================================================================

    @Test
    @DisplayName("ProfileSearch: Citizen searching for their own UUID should succeed")
    public void testProfileSearch_CitizenOwnUuid_Succeeds() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pb.amritsar")
                .uuid(Collections.singletonList("citizen-uuid-1"))
                .requestInfo(createCitizenRequestInfo("citizen-uuid-1", "9876543210"))
                .build();

        assertDoesNotThrow(() -> profileValidator.validateSearch(request));
    }

    @Test
    @DisplayName("ProfileSearch: Citizen searching for their own mobile should succeed")
    public void testProfileSearch_CitizenOwnMobile_Succeeds() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pb.amritsar")
                .mobileNumber("9876543210")
                .requestInfo(createCitizenRequestInfo("citizen-uuid-1", "9876543210"))
                .build();

        assertDoesNotThrow(() -> profileValidator.validateSearch(request));
    }

    @Test
    @DisplayName("ProfileSearch: Citizen searching for someone else's UUID should fail")
    public void testProfileSearch_CitizenOtherUuid_ThrowsUnauthorized() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pb.amritsar")
                .uuid(Collections.singletonList("different-citizen-uuid"))
                .requestInfo(createCitizenRequestInfo("citizen-uuid-1", "9876543210"))
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> profileValidator.validateSearch(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, ex.getCode());
        assertTrue(ex.getMessage().contains("Citizens are only permitted to search for their own profile"));
    }

    @Test
    @DisplayName("ProfileSearch: Citizen searching for list containing another user's UUID should fail")
    public void testProfileSearch_CitizenUuidListWithOtherUser_ThrowsUnauthorized() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pb.amritsar")
                .uuid(Arrays.asList("citizen-uuid-1", "other-citizen-uuid"))
                .requestInfo(createCitizenRequestInfo("citizen-uuid-1", "9876543210"))
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> profileValidator.validateSearch(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, ex.getCode());
    }

    @Test
    @DisplayName("ProfileSearch: Citizen searching for someone else's mobile should fail")
    public void testProfileSearch_CitizenOtherMobile_ThrowsUnauthorized() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pb.amritsar")
                .mobileNumber("9111111111")
                .requestInfo(createCitizenRequestInfo("citizen-uuid-1", "9876543210"))
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> profileValidator.validateSearch(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, ex.getCode());
    }

    @Test
    @DisplayName("ProfileSearch: Citizen searching by name only without own UUID/mobile should fail")
    public void testProfileSearch_CitizenNameOnly_ThrowsUnauthorized() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pb.amritsar")
                .name("Aarav Sharma")
                .requestInfo(createCitizenRequestInfo("citizen-uuid-1", "9876543210"))
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> profileValidator.validateSearch(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, ex.getCode());
    }

    @Test
    @DisplayName("ProfileSearch: EMPLOYEE searching for a citizen should succeed")
    public void testProfileSearch_EmployeeSearchingCitizen_Succeeds() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pb.amritsar")
                .uuid(Collections.singletonList("citizen-uuid-1"))
                .mobileNumber("9876543210")
                .requestInfo(createEmployeeRequestInfo("emp-uuid-1"))
                .build();

        assertDoesNotThrow(() -> profileValidator.validateSearch(request));
    }

    // =========================================================================
    // 2. UserService (Used by /users/_search and /users/v2/_search)
    // =========================================================================

    @Test
    @DisplayName("UserService.searchUsers: Citizen searching for someone else's UUID should fail")
    public void testUserServiceSearch_CitizenOtherUuid_ThrowsUnauthorized() {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId("pg")
                .uuid(Collections.singletonList("other-uuid"))
                .build();

        RequestInfo requestInfo = createCitizenRequestInfo("citizen-uuid-1", "9876543210");

        CustomException ex = assertThrows(CustomException.class, () ->
                userService.searchUsers(criteria, false, requestInfo)
        );
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, ex.getCode());
    }

    @Test
    @DisplayName("UserService.searchUsers: Citizen searching for someone else's mobile should fail")
    public void testUserServiceSearch_CitizenOtherMobile_ThrowsUnauthorized() {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId("pg")
                .mobileNumber("9999999999")
                .build();

        RequestInfo requestInfo = createCitizenRequestInfo("citizen-uuid-1", "9876543210");

        CustomException ex = assertThrows(CustomException.class, () ->
                userService.searchUsers(criteria, false, requestInfo)
        );
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, ex.getCode());
    }

    @Test
    @DisplayName("UserService.searchUsers: Citizen searching for their own UUID should succeed")
    public void testUserServiceSearch_CitizenOwnUuid_Succeeds() {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId("pg")
                .uuid(Collections.singletonList("citizen-uuid-1"))
                .build();

        RequestInfo requestInfo = createCitizenRequestInfo("citizen-uuid-1", "9876543210");

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> userService.searchUsers(criteria, false, requestInfo));
    }

    @Test
    @DisplayName("UserService.searchUsersV2: Citizen searching for another user should fail")
    public void testUserServiceSearchV2_CitizenOtherUser_ThrowsUnauthorized() {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId("pg")
                .uuid(Collections.singletonList("other-user-uuid"))
                .build();

        RequestInfo requestInfo = createCitizenRequestInfo("citizen-uuid-1", "9876543210");

        CustomException ex = assertThrows(CustomException.class, () ->
                userService.searchUsersV2(criteria, false, requestInfo)
        );
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, ex.getCode());
    }

    @Test
    @DisplayName("UserService.searchUsers: Inter-service call should bypass citizen self-search restriction")
    public void testUserServiceSearch_InterServiceCall_BypassesCitizenRestriction() {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId("pg")
                .uuid(Collections.singletonList("any-uuid"))
                .build();

        // Citizen RequestInfo passed through internal microservice call
        RequestInfo requestInfo = createCitizenRequestInfo("citizen-uuid-1", "9876543210");

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> userService.searchUsers(criteria, true, requestInfo));
    }

    // =========================================================================
    // 3. Database Ground Truth Enforcement (Cannot Spoof via RequestInfo)
    // =========================================================================

    @Test
    @DisplayName("UserService.searchUsers: Spoofed caller type EMPLOYEE detected as CITIZEN in DB should fail searching others")
    public void testUserServiceSearch_SpoofedCallerTypeEmployee_DetectedAsCitizenFromDb_ThrowsUnauthorized() {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId("pg")
                .uuid(Collections.singletonList("other-citizen-uuid"))
                .build();

        // Caller claims to be EMPLOYEE in RequestInfo
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(777L)
                        .uuid("spoofed-citizen-uuid")
                        .userName("spoofed")
                        .type("EMPLOYEE")
                        .build())
                .build();

        // But in the DB, this user is a CITIZEN
        User dbCaller = User.builder()
                .id(777L)
                .uuid("spoofed-citizen-uuid")
                .username("9998887770")
                .mobileNumber("9998887770")
                .type(UserType.CITIZEN)
                .tenantId("pg")
                .roles(Collections.singleton(Role.getCitizenRole()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(dbCaller));

        CustomException ex = assertThrows(CustomException.class, () ->
                userService.searchUsers(criteria, false, requestInfo)
        );
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, ex.getCode());
    }

    @Test
    @DisplayName("UserService.updateWithoutOtpValidation: Spoofed caller type EMPLOYEE detected as CITIZEN in DB should fail")
    public void testUserServiceUpdateWithoutOtpValidation_SpoofedCallerTypeEmployee_DetectedAsCitizenFromDb_ThrowsUnauthorized() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(888L)
                        .uuid("citizen-in-db-uuid")
                        .type("EMPLOYEE")
                        .build())
                .build();

        User dbCaller = User.builder()
                .id(888L)
                .uuid("citizen-in-db-uuid")
                .username("9998887771")
                .type(UserType.CITIZEN)
                .tenantId("pg")
                .roles(Collections.singleton(Role.getCitizenRole()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(dbCaller));

        User targetUser = User.builder()
                .id(888L)
                .uuid("citizen-in-db-uuid")
                .tenantId("pg")
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                userService.updateWithoutOtpValidation(targetUser, requestInfo)
        );
        assertEquals("UNAUTHORIZED_ACCESS", ex.getCode());
    }

    @Test
    @DisplayName("UserService.updateUserV2: Citizen caller attempting to update another citizen should fail")
    public void testUserServiceUpdateV2_CitizenAttemptsUpdateOtherUser_ThrowsUnauthorized() {
        RequestInfo requestInfo = createCitizenRequestInfo("citizen-uuid-1", "9876543210");

        User existingDbUser = User.builder()
                .id(999L)
                .uuid("other-user-uuid")
                .username("9111111111")
                .mobileNumber("9111111111")
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .roles(Collections.singleton(Role.getCitizenRole()))
                .accountLocked(false)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));

        User incomingUpdate = User.builder()
                .id(999L)
                .uuid("other-user-uuid")
                .tenantId("pg")
                .name("New Name")
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                userService.updateUserV2(incomingUpdate, requestInfo)
        );
        assertEquals("UNAUTHORIZED_ACCESS", ex.getCode());
    }

    @Test
    @DisplayName("UserService.updateUserV2: Citizen caller updating own profile should succeed")
    public void testUserServiceUpdateV2_CitizenUpdatesSelf_Succeeds() {
        RequestInfo requestInfo = createCitizenRequestInfo("citizen-uuid-1", "9876543210");

        User existingDbUser = User.builder()
                .id(101L)
                .uuid("citizen-uuid-1")
                .username("9876543210")
                .mobileNumber("9876543210")
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .roles(Collections.singleton(Role.getCitizenRole()))
                .accountLocked(false)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));

        User incomingUpdate = User.builder()
                .id(101L)
                .uuid("citizen-uuid-1")
                .username("9876543210")
                .mobileNumber("9876543210")
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .roles(Collections.singleton(Role.getCitizenRole()))
                .build();

        assertDoesNotThrow(() -> userService.updateUserV2(incomingUpdate, requestInfo));
        verify(userRepository).updateV2(any(User.class), eq(existingDbUser), eq(101L), eq("citizen-uuid-1"));
    }

    @Test
    @DisplayName("ProfileValidator.validateUpdate: Citizen caller attempting to update another citizen should fail")
    public void testProfileValidatorUpdate_CitizenAttemptsUpdateOtherUser_ThrowsUnauthorized() {
        RequestInfo requestInfo = createCitizenRequestInfo("citizen-uuid-1", "9876543210");

        User existingDbUser = User.builder()
                .id(999L)
                .uuid("target-citizen-uuid")
                .type(UserType.CITIZEN)
                .build();

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .requestInfo(requestInfo)
                .user(UserRequest.builder()
                        .id(999L)
                        .uuid("target-citizen-uuid")
                        .build())
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                profileValidator.validateUpdate(request, existingDbUser)
        );
        assertEquals("UNAUTHORIZED_ACCESS", ex.getCode());
    }
}
