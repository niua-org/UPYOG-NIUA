package org.upyog.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.upyog.mcp.config.McpProperties;

/**
 * Builds {@code POST /user/_search} bodies to match the NIUA citizen UI curl
 * ({@code tenantId}, {@code uuid[]}, {@code pageSize}, {@code RequestInfo.authToken} only).
 * {@code RequestInfo.userInfo} is included only when the caller sends {@code x-user-info}.
 */
public class EgovUserSearchRequestBuilder {

    private final ObjectMapper objectMapper;
    private final McpProperties properties;

    public EgovUserSearchRequestBuilder(ObjectMapper objectMapper, McpProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * @param accessToken   session token ({@code auth-token} / {@code Auth-Token})
     * @param userUuid      citizen uuid (also in {@code uuid[]} body field)
     * @param userInfo      optional full {@code RequestInfo.userInfo} from {@code x-user-info} header
     */
    public ObjectNode build(String accessToken, String userUuid, JsonNode userInfo) {
        ObjectNode body = objectMapper.createObjectNode();
        String tenantId = properties.getUserSessionTenantId();
        if (userInfo != null && !userInfo.path("tenantId").asText("").isBlank()) {
            tenantId = userInfo.path("tenantId").asText();
        }
        body.put("tenantId", tenantId);
        body.put("pageSize", "100");
        ArrayNode uuids = body.putArray("uuid");
        uuids.add(userUuid);

        ObjectNode requestInfo = objectMapper.createObjectNode();
        requestInfo.put("apiId", properties.getRequestInfoApiId());
        requestInfo.put("authToken", accessToken);
        requestInfo.put("msgId", System.currentTimeMillis() + "|en_IN");
        requestInfo.set("plainAccessRequest", objectMapper.createObjectNode());
        // egov-user /user/_search requires RequestInfo.userInfo — always include it.
        // Use the full x-user-info when provided; otherwise build a minimal stub from uuid.
        if (userInfo != null && !userInfo.isNull()) {
            requestInfo.set("userInfo", userInfo.deepCopy());
        } else {
            requestInfo.set("userInfo", minimalUserInfo(userUuid, tenantId));
        }
        body.set("RequestInfo", requestInfo);
        return body;
    }

    /**
     * Builds the minimal {@code RequestInfo.userInfo} required when the caller passes only
     * {@code x-user-uuid} (no full {@code x-user-info} header). Matches the structure the
     * UPYOG citizen UI always includes in its {@code /user/_search} requests.
     */
    private ObjectNode minimalUserInfo(String uuid, String tenantId) {
        ObjectNode ui = objectMapper.createObjectNode();
        ui.put("uuid", uuid);
        ui.put("type", "CITIZEN");
        ui.put("tenantId", tenantId);
        ArrayNode roles = ui.putArray("roles");
        ObjectNode role = roles.addObject();
        role.put("name", "Citizen");
        role.put("code", "CITIZEN");
        role.put("tenantId", tenantId);
        ui.put("active", true);
        return ui;
    }

}
