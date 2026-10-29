package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.service.UserService;
import org.egov.user.web.contract.LoggedInUserUpdatePasswordRequest;
import org.egov.user.web.contract.NonLoggedInUserUpdatePasswordRequest;
import org.egov.user.web.contract.UpdatePasswordResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private PasswordController controller;

    @Test
    void updatePassword_loggedInUser_delegatesToService() {
        LoggedInUserUpdatePasswordRequest request = new LoggedInUserUpdatePasswordRequest();
        request.setExistingPassword("old");
        request.setNewPassword("new");

        UpdatePasswordResponse response = controller.updatePassword(request);

        verify(userService).updatePasswordForLoggedInUser(request.toDomain());
        assertThat(response.getResponseInfo().getStatus()).isEqualTo("200");
    }

    @Test
    void updatePassword_nonLoggedInUser_delegatesToService() {
        NonLoggedInUserUpdatePasswordRequest request = new NonLoggedInUserUpdatePasswordRequest();
        request.setRequestInfo(RequestInfo.builder().build());
        request.setOtpReference("otp-ref");
        request.setNewPassword("new");

        UpdatePasswordResponse response = controller.updatePasswordForNonLoggedInUser(request);

        verify(userService).updatePasswordForNonLoggedInUser(request.toDomain(), request.getRequestInfo());
        assertThat(response.getResponseInfo().getStatus()).isEqualTo("200");
    }
}
