package com.example.gateway.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Shared helpers for routing and authenticating the UPYOG MCP server behind the API gateway.
 * <p>
 * MCP is mounted at {@link #PATH_PREFIX} (see {@code routes.properties} StripPrefix to {@code /mcp}).
 * Requests use Streamable HTTP JSON-RPC and do not carry {@code RequestInfo} in the POST body, unlike
 * Rainmaker business APIs. Filters branch on {@link #isMcpPath(String)} to use header-based auth and
 * {@link #GATEWAY_REQUEST_INFO_ATTR} for RBAC instead of {@code ModifyRequestBody} enrichment.
 */
public final class UpyogMcpGatewaySupport {

    /** Gateway route prefix for {@code upyog-mcp-server} (chatbot / assistant entry). */
    public static final String PATH_PREFIX = "/upyog-mcp-server";

    /**
     * Exchange attribute key: {@link org.egov.common.contract.request.RequestInfo} built after MCP token auth
     * (used by {@link com.example.gateway.filters.pre.helpers.RbacFilterHelper}).
     */
    public static final String GATEWAY_REQUEST_INFO_ATTR = "gateway.requestInfo";

    private UpyogMcpGatewaySupport() {
    }

    /** @return {@code true} when the path is the MCP server route (auth/RBAC use MCP-specific handling) */
    public static boolean isMcpPath(String path) {
        return path != null && path.startsWith(PATH_PREFIX);
    }

    /**
     * Reads the UPYOG session token from gateway headers ({@code Auth-Token} or {@code auth-token}).
     *
     * @param headers incoming request headers from chatbot or MCP client
     * @return trimmed token, or {@code null} if absent
     */
    public static String firstAuthToken(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String token = firstHeader(headers, "Auth-Token");
        if (ObjectUtils.isEmpty(token)) {
            token = firstHeader(headers, "auth-token");
        }
        return token;
    }

    private static String firstHeader(HttpHeaders headers, String name) {
        List<String> values = headers.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        String value = values.get(0);
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
