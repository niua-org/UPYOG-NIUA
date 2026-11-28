package org.upyog.mcp.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.mcp.client.UserSessionClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Resolves citizen identity headers for {@code POST /user/_search} (same as NIUA citizen UI).
 */
public final class UserUuidResolver {

    private static final Logger log = LoggerFactory.getLogger(UserUuidResolver.class);

    /** Base64 (standard) of UTF-8 {@code x-user-info} when the JSON exceeds servlet header limits. */
    public static final String USER_INFO_BASE64_HEADER = "x-user-info-base64";

    private UserUuidResolver() {
    }

    /**
     * @return uuid from {@code x-user-uuid} or {@code x-user-info} JSON ({@code uuid} field)
     */
    public static String fromRequest(HttpServletRequest request, ObjectMapper objectMapper) {
        String direct = request.getHeader(UserSessionClient.USER_UUID_HEADER);
        if (direct != null && !direct.isBlank()) {
            return direct.trim();
        }
        JsonNode userInfo = userInfoFromRequest(request, objectMapper);
        if (userInfo != null) {
            return userInfo.path("uuid").asText("").trim();
        }
        return "";
    }

    /**
     * @return parsed {@code x-user-info} JSON for {@code RequestInfo.userInfo}, or null
     */
    public static JsonNode userInfoFromRequest(HttpServletRequest request, ObjectMapper objectMapper) {
        String userInfoJson = request.getHeader(GatewayIdentityHeaders.USER_INFO);
        if (userInfoJson == null || userInfoJson.isBlank()) {
            userInfoJson = decodeUserInfoBase64(request.getHeader(USER_INFO_BASE64_HEADER));
        }
        if (userInfoJson == null || userInfoJson.isBlank()) {
            return null;
        }
        try {
            JsonNode user = objectMapper.readTree(userInfoJson);
            if (user.path("uuid").asText("").isBlank()) {
                log.warn("x-user-info ignored: uuid field missing or blank");
                return null;
            }
            return user;
        } catch (Exception exception) {
            log.warn("x-user-info parse failed: {}", exception.getMessage());
            return null;
        }
    }

    private static String decodeUserInfoBase64(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(encoded.trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            log.warn("x-user-info-base64 decode failed: {}", exception.getMessage());
            return null;
        }
    }
}
