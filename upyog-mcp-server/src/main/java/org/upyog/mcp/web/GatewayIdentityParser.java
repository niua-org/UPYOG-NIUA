package org.upyog.mcp.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link AuthenticatedUser} from gateway-forwarded headers after the gateway validated RBAC.
 */
public final class GatewayIdentityParser {

    private GatewayIdentityParser() {
    }

    /**
     * @return user when {@code x-pass-through-gateway} and {@code x-user-info} are present; otherwise null
     */
    public static AuthenticatedUser fromGatewayHeaders(HttpServletRequest request, ObjectMapper objectMapper) {
        if (!"true".equalsIgnoreCase(request.getHeader(GatewayIdentityHeaders.PASS_THROUGH))) {
            return null;
        }
        String userJson = request.getHeader(GatewayIdentityHeaders.USER_INFO);
        if (userJson == null || userJson.isBlank()) {
            return null;
        }
        String authToken = firstToken(request);
        if (authToken == null || authToken.isBlank()) {
            return null;
        }
        try {
            JsonNode user = objectMapper.readTree(userJson);
            String uuid = user.path("uuid").asText("");
            if (uuid.isBlank()) {
                return null;
            }
            List<String> roleCodes = new ArrayList<>();
            List<String> roleTenants = new ArrayList<>();
            for (JsonNode role : user.path("roles")) {
                roleCodes.add(role.path("code").asText(""));
                roleTenants.add(role.path("tenantId").asText(""));
            }
            return new AuthenticatedUser(
                    uuid,
                    user.path("type").asText(""),
                    user.path("tenantId").asText(""),
                    roleCodes,
                    roleTenants,
                    authToken
            );
        } catch (Exception exception) {
            return null;
        }
    }

    static String firstToken(HttpServletRequest request) {
        String token = request.getHeader(GatewayIdentityHeaders.AUTH_TOKEN);
        if (token == null || token.isBlank()) {
            token = request.getHeader(AuthTokenFilter.HEADER);
        }
        return token;
    }
}
