package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.UpdateResponse;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.TokenService;
import org.egov.user.domain.service.UserService;
import org.egov.user.web.contract.CreateUserRequest;
import org.egov.user.web.contract.UserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRequestControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private TokenService tokenService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService, tokenService);
    }

    @Test
    void updateWithoutOtpValidation_delegatesToUserServiceAndReturnsResponse() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder().uuid("actor").id(1L).build())
                .build();
        UserRequest userRequest = UserRequest.builder()
                .uuid("uuid-1")
                .userName("jamaalbhai")
                .name("Jamaal Bhai")
                .type(UserType.CITIZEN)
                .tenantId("pb")
                .active(true)
                .roles(Set.of())
                .build();
        CreateUserRequest createUserRequest = new CreateUserRequest(requestInfo, userRequest);

        User updated = User.builder().uuid("uuid-1").username("jamaalbhai").name("Jamaal Bhai").build();
        when(userService.updateWithoutOtpValidation(any(User.class), eq(requestInfo))).thenReturn(updated);

        UpdateResponse response = controller.updateUserWithoutValidation(createUserRequest, new HttpHeaders());

        assertThat(response.getUser()).isNotEmpty();
        verify(userService).updateWithoutOtpValidation(any(User.class), eq(requestInfo));
    }
}
