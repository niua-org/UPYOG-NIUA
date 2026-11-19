package org.egov.user.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.CreateUserRequest;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.controller.UserController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateWithoutOtpValidationSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;
    private UserController userController;

    @BeforeEach
    public void setUp() {
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
        userController = new UserController(userService, null);
    }

    @Test
    @DisplayName("Should allow updateWithoutOtpValidation when target user is CITIZEN, matching createnovalidate behavior")
    public void testUpdateWithoutOtpValidation_AllowsCitizenTargetUser() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(501L)
                        .uuid("employee-uuid")
                        .type("EMPLOYEE")
                        .build())
                .build();

        User existingDbUser = User.builder()
                .id(101L)
                .uuid("citizen-uuid")
                .username("9876543210")
                .type(UserType.CITIZEN)
                .tenantId("pg")
                .password("encoded-pwd")
                .roles(Collections.singleton(Role.getCitizenRole()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));
        when(encryptionDecryptionUtil.encryptObject(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        User updatePayload = User.builder()
                .id(101L)
                .uuid("citizen-uuid")
                .name("Updated Citizen")
                .type(UserType.CITIZEN)
                .tenantId("pg")
                .roles(Collections.singleton(Role.getCitizenRole()))
                .build();

        User result = userService.updateWithoutOtpValidation(updatePayload, requestInfo);

        assertNotNull(result);
        verify(userRepository, times(1)).update(any(User.class), any(User.class), anyLong(), nullable(String.class));
    }

    @Test
    @DisplayName("Should allow updateWithoutOtpValidation when caller is CITIZEN")
    public void testUpdateWithoutOtpValidation_AllowsCitizenCaller() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(101L)
                        .uuid("citizen-uuid")
                        .type("CITIZEN")
                        .build())
                .build();

        User existingDbUser = User.builder()
                .id(101L)
                .uuid("citizen-uuid")
                .username("9876543210")
                .type(UserType.CITIZEN)
                .tenantId("pg")
                .password("encoded-pwd")
                .roles(Collections.singleton(Role.getCitizenRole()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));
        when(encryptionDecryptionUtil.encryptObject(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        User updatePayload = User.builder()
                .id(101L)
                .uuid("citizen-uuid")
                .name("Updated Citizen By Self")
                .type(UserType.CITIZEN)
                .tenantId("pg")
                .roles(Collections.singleton(Role.getCitizenRole()))
                .build();

        User result = userService.updateWithoutOtpValidation(updatePayload, requestInfo);

        assertNotNull(result);
        verify(userRepository, times(1)).update(any(User.class), any(User.class), anyLong(), nullable(String.class));
    }

    @Test
    @DisplayName("Should reject updateWithoutOtpValidation when provided ID mismatches DB ID")
    public void testUpdateWithoutOtpValidation_WhenIdMismatch_ThrowsDataMismatch() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(501L)
                        .uuid("updater-uuid")
                        .type("EMPLOYEE")
                        .build())
                .build();

        User existingDbUser = User.builder()
                .id(101L)
                .uuid("target-uuid")
                .username("9876543210")
                .type(UserType.CITIZEN)
                .tenantId("pg")
                .roles(Collections.singleton(Role.getCitizenRole()))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));

        User updatePayload = User.builder()
                .id(999L) // Mismatched ID
                .uuid("target-uuid")
                .type(UserType.CITIZEN)
                .tenantId("pg")
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                userService.updateWithoutOtpValidation(updatePayload, requestInfo)
        );

        assertEquals(UserConstants.ERROR_DATA_MISMATCH, ex.getCode());
    }
}
