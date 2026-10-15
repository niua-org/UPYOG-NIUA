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
        if (userInfo != null && !userInfo.isNull()) {
            requestInfo.set("userInfo", userInfo.deepCopy());
        }
        body.set("RequestInfo", requestInfo);
        return body;
    }
}
