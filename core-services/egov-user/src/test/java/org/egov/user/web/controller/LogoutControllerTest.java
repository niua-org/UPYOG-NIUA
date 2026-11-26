package org.egov.user.web.controller;

import org.egov.common.contract.response.ErrorResponse;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.TokenWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutControllerTest {

    @Mock
    private OAuth2AuthorizationService authorizationService;

    private LogoutController controller;

    @BeforeEach
    void setUp() {
        controller = new LogoutController(authorizationService);
    }

    @Test
    void deleteToken_missingToken_throwsIllegalArgumentException() {
        TokenWrapper wrapper = new TokenWrapper();
        wrapper.setAccessToken("  ");

        assertThatThrownBy(() -> controller.deleteToken(wrapper))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteToken_knownAuthorization_removesAndReturnsSuccess() throws Exception {
        TokenWrapper wrapper = new TokenWrapper();
        wrapper.setAccessToken("access-token");
        OAuth2Authorization authorization = mock(OAuth2Authorization.class);
        when(authorizationService.findByToken("access-token", OAuth2TokenType.ACCESS_TOKEN))
                .thenReturn(authorization);

        ResponseInfo response = controller.deleteToken(wrapper);

        verify(authorizationService).remove(authorization);
        assertThat(response.getStatus()).contains("Logout successfully");
    }

    @Test
    void deleteToken_unknownToken_returnsNotFoundMessage() throws Exception {
        TokenWrapper wrapper = new TokenWrapper();
        wrapper.setAccessToken("missing");
        when(authorizationService.findByToken(eq("missing"), eq(OAuth2TokenType.ACCESS_TOKEN)))
                .thenReturn(null);

        ResponseInfo response = controller.deleteToken(wrapper);

        assertThat(response.getStatus()).contains("not found");
    }

    @Test
    void handleError_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response = controller.handleError(new RuntimeException("fail"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getDescription()).contains("fail");
    }
}
