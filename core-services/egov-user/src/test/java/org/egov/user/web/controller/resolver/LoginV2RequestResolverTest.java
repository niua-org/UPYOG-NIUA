package org.egov.user.web.controller.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginV2RequestResolverTest {

    private final LoginV2RequestResolver resolver = new LoginV2RequestResolver();

    @Mock
    private HttpServletRequest servletRequest;

    @Test
    void resolve_familyIdFromQueryParameter_bindsToRequest() {
        when(servletRequest.getParameter("familyId")).thenReturn("HR-PPP-998877");
        when(servletRequest.getParameter("tenantId")).thenReturn("pb");
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        LoginV2Request resolved = resolver.resolve(new LoginV2Request(), servletRequest);

        assertThat(resolved.getFamilyId()).isEqualTo("HR-PPP-998877");
        assertThat(resolved.getTenantId()).isEqualTo("pb");
        assertThat(resolved.getRequestInfo()).isNotNull();
    }

    @Test
    void resolveClientIp_xForwardedFor_usesFirstHop() {
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1");

        assertThat(resolver.resolveClientIp(servletRequest)).isEqualTo("203.0.113.1");
    }

    @Test
    void resolve_nullBody_buildsSyntheticRequestInfo() {
        LoginV2Request resolved = resolver.resolve(null, null);

        assertThat(resolved.getRequestInfo()).isNotNull();
        assertThat(resolved.getRequestInfo().getApiId()).isEqualTo("egov-user-loginv2");
    }

    @Test
    void resolve_preservesAuthTypeFromBody() {
        LoginV2Request body = LoginV2Request.builder().authType(AuthType.FAMILY_ID).build();

        LoginV2Request resolved = resolver.resolve(body, null);

        assertThat(resolved.getAuthType()).isEqualTo(AuthType.FAMILY_ID);
    }
}
