package org.upyog.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * HTTP client that talks only to the UPYOG API gateway.
 * Callers pass a path from a descriptor or a fixed server constant, never a caller-supplied URL.
 */
public interface GatewayClient {

    /**
     * POST JSON to a fixed gateway-relative path.
     *
     * @param gatewayPath path from a descriptor (never caller-supplied host)
     * @param query       repeated query parameters
     * @param body        full request including {@code RequestInfo}
     * @param read        when {@code true}, retries and read timeouts apply; writes are never retried
     */
    JsonNode postJson(String gatewayPath, Map<String, List<String>> query, JsonNode body, boolean read);

    /**
     * POST JSON with extra headers (for example {@code Auth-Token} on {@code /user/_details}).
     */
    default JsonNode postJson(String gatewayPath, Map<String, List<String>> query, JsonNode body, boolean read,
                              Map<String, String> headers) {
        return postJson(gatewayPath, query, body, read);
    }

    /**
     * POST JSON without injecting any {@code Auth-Token} header.
     * Use for endpoints like {@code /user/_search} where the UPYOG citizen UI proves auth is
     * sent only in {@code RequestInfo.authToken} in the body — adding the header triggers
     * gateway RBAC which blocks CITIZEN-role tokens.
     */
    default JsonNode postJsonNoToken(String gatewayPath, Map<String, List<String>> query, JsonNode body, boolean read) {
        return postJson(gatewayPath, query, body, read);
    }

    /**
     * Uploads a document to filestore before a confirmed write.
     *
     * @param module filestore module from the descriptor ({@code PGR}, …)
     */
    JsonNode postFile(String gatewayPath, String tenantId, String module, String fileName,
                      String contentType, byte[] content);
}
