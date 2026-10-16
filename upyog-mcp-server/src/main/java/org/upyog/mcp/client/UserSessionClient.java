package org.upyog.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.upyog.mcp.config.McpProperties;
import org.upyog.mcp.web.AuthenticatedUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates MCP session via {@code POST /user/_search} only (NIUA citizen UI contract).
 */
@Component
public class UserSessionClient {

    private static final Logger log = LoggerFactory.getLogger(UserSessionClient.class);

    /** Citizen uuid when {@code x-user-info} is not sent. */
    public static final String USER_UUID_HEADER = "x-user-uuid";

    private final GatewayClient gatewayClient;
    private final EgovUserSearchRequestBuilder searchRequestBuilder;
    private final McpProperties properties;
    private final ObjectMapper objectMapper;

    public UserSessionClient(GatewayClient gatewayClient, McpProperties properties, ObjectMapper objectMapper) {
        this.gatewayClient = gatewayClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.searchRequestBuilder = new EgovUserSearchRequestBuilder(objectMapper, properties);
    }

    /**
     * @param sessionToken UPYOG access token
     * @param userUuid     from {@code x-user-uuid} or {@code x-user-info.uuid}
     * @param userInfo     optional full {@code x-user-info} JSON (recommended; matches UI)
     */
    public AuthenticatedUser resolve(String sessionToken, String userUuid, JsonNode userInfo) {
        String token = normalizeToken(sessionToken);
        String uuid = userUuid == null ? "" : userUuid.trim();
        if (uuid.isBlank() && userInfo != null) {
            uuid = userInfo.path("uuid").asText("").trim();
        }
        log.info("user_session_start gatewayBase={} uuidPresent={} userInfoHeaderPresent={}",
                properties.getGatewayBaseUrl(), !uuid.isBlank(), userInfo != null && !userInfo.isNull());
        if (uuid.isBlank()) {
            throw new IllegalStateException(
                    "Citizen uuid is required. Pass x-user-info (full UI userInfo JSON) or x-user-uuid on the MCP request.");
        }
        JsonNode user = resolveViaUserSearch(token, uuid, userInfo);
        return toAuthenticatedUser(user, token);
    }

    private JsonNode resolveViaUserSearch(String token, String uuid, JsonNode userInfo) {
        JsonNode body = searchRequestBuilder.build(token, uuid, userInfo);
        // Citizen UI sends auth only in RequestInfo.authToken body field — no Auth-Token header.
        // Sending Auth-Token header triggers gateway RBAC which blocks CITIZEN role on /user/_search.
        GatewayHttpDebugLog.logPostWithReplayCurl(
                log,
                "gateway_user_search",
                properties.getGatewayBaseUrl(),
                EgovUserContracts.SEARCH_PATH,
                Map.of(),
                body,
                Map.of(),
                objectMapper);
        JsonNode response = gatewayClient.postJsonNoToken(
                EgovUserContracts.SEARCH_PATH,
                Map.of(),
                body,
                true);
        log.info("gateway_user_search responsePreview={}", previewJson(response));
        rejectIfErrors(response);
        JsonNode first = EgovUserSearchResponseParser.firstUser(response);
        if (first.isMissingNode() || first.isNull()) {
            if (userInfo != null && searchSucceeded(response)) {
                log.warn("user_session_search_empty_using_user_info_header uuid={}", uuid);
                return userInfo;
            }
            log.warn("user_session_empty_search_response keys={}", fieldNames(response));
            throw new IllegalStateException(
                    "No user returned from POST /user/_search (expected JSON field 'user'). "
                            + "Pass x-user-uuid or a valid x-user-info / x-user-info-base64 header on the MCP request.");
        }
        return first;
    }

    private AuthenticatedUser toAuthenticatedUser(JsonNode user, String token) {
        String uuid = user.path("uuid").asText("");
        if (uuid.isBlank()) {
            throw new IllegalStateException("user uuid missing in egov-user response");
        }
        List<String> roles = new ArrayList<>();
        List<String> roleTenants = new ArrayList<>();
        for (JsonNode role : user.path("roles")) {
            roles.add(role.path("code").asText(""));
            roleTenants.add(role.path("tenantId").asText(""));
        }
        String tenantId = user.path("tenantId").asText("");
        if (tenantId.isBlank()) {
            tenantId = properties.getUserSessionTenantId();
        }
        String type = user.path("type").asText("");
        if (type.isBlank() && user.path("type").isObject()) {
            type = user.path("type").path("name").asText("");
        }
        return new AuthenticatedUser(uuid, type, tenantId, roles, roleTenants, token);
    }

    public static String normalizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        String token = raw.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        return token;
    }

    private void rejectIfErrors(JsonNode root) {
        JsonNode errors = root.get("Errors");
        if (errors != null && errors.isArray() && !errors.isEmpty()) {
            String message = errors.get(0).path("message").asText("Invalid access token");
            log.warn("user_session_rejected reason={}", message);
            throw new IllegalStateException(message);
        }
    }

    private static boolean searchSucceeded(JsonNode response) {
        String status = response.path("responseInfo").path("status").asText("");
        return "200".equals(status) || status.isBlank();
    }

    private static String fieldNames(JsonNode response) {
        StringBuilder names = new StringBuilder("[");
        response.fieldNames().forEachRemaining(name -> {
            if (names.length() > 1) {
                names.append(", ");
            }
            names.append(name);
        });
        names.append(']');
        return names.toString();
    }

    private String previewJson(JsonNode node) {
        try {
            String raw = objectMapper.writeValueAsString(node);
            return raw.length() <= 400 ? raw : raw.substring(0, 400) + "…";
        } catch (Exception exception) {
            return String.valueOf(node);
        }
    }
}
