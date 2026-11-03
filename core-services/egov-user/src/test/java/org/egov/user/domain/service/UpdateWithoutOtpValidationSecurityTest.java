package org.egov.user.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
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
    @DisplayName("Should reject updateWithoutOtpValidation when caller is CITIZEN in UserService")
    public void testUpdateWithoutOtpValidation_WhenCallerIsCitizen_ThrowsUnauthorized() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(101L)
                        .uuid("citizen-uuid")
                        .type("CITIZEN")
                        .build())
                .build();

        User user = User.builder()
                .id(101L)
                .uuid("citizen-uuid")
                .type(UserType.CITIZEN)
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                userService.updateWithoutOtpValidation(user, requestInfo)
        );

        assertEquals("UNAUTHORIZED_ACCESS", ex.getCode());
        assertTrue(ex.getMessage().contains("CITIZEN"));
    }

    @Test
    @DisplayName("Should reject /users/_updatenovalidate controller endpoint when caller is CITIZEN")
    public void testController_WhenCallerIsCitizen_ThrowsUnauthorized() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(101L)
                        .uuid("citizen-uuid")
                        .type("CITIZEN")
                        .build())
                .build();

        UserRequest userRequest = UserRequest.builder()
                .id(101L)
                .uuid("citizen-uuid")
                .type(UserType.EMPLOYEE)
                .build();

        CreateUserRequest request = new CreateUserRequest(requestInfo, userRequest, null);

        CustomException ex = assertThrows(CustomException.class, () ->
                userController.updateUserWithoutValidation(request, new HttpHeaders())
        );

        assertEquals("UNAUTHORIZED_ACCESS", ex.getCode());
    }

    @Test
    @DisplayName("Should reject /users/_updatenovalidate controller endpoint when target user type is CITIZEN")
    public void testController_WhenTargetUserIsCitizen_ThrowsInvalidUserType() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(501L)
                        .uuid("employee-uuid")
                        .type("EMPLOYEE")
                        .build())
                .build();

        UserRequest userRequest = UserRequest.builder()
                .id(101L)
                .uuid("citizen-uuid")
                .type(UserType.CITIZEN)
                .build();

        CreateUserRequest request = new CreateUserRequest(requestInfo, userRequest, null);

        CustomException ex = assertThrows(CustomException.class, () ->
                userController.updateUserWithoutValidation(request, new HttpHeaders())
        );

        assertEquals("INVALID_USER_TYPE", ex.getCode());
    }
}
