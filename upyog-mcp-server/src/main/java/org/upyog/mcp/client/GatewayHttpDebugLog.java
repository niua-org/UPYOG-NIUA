package org.upyog.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Logs replayable gateway/auth HTTP calls for local debugging (egov-user session validation).
 */
public final class GatewayHttpDebugLog {

    private GatewayHttpDebugLog() {
    }

    public static void logPost(Logger log, String label, String baseUrl, String path,
                               Map<String, List<String>> query, JsonNode body,
                               Map<String, String> headers, ObjectMapper objectMapper) {
        String url = buildUrl(baseUrl, path, query);
        String payload = formatBody(body, objectMapper);
        String curl = toCurl(url, headers, payload);
        log.info("{} url={} payload={} curl={}", label, url, payload, curl);
    }

    static String buildUrl(String baseUrl, String path, Map<String, List<String>> query) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        StringBuilder url = new StringBuilder(base).append(normalizedPath);
        if (query != null && !query.isEmpty()) {
            String q = query.entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream()
                            .filter(value -> value != null && !value.isBlank())
                            .map(value -> entry.getKey() + "=" + urlEncode(value)))
                    .collect(Collectors.joining("&"));
            if (!q.isEmpty()) {
                url.append("?").append(q);
            }
        }
        return url.toString();
    }

    static String toCurl(String url, Map<String, String> headers, String jsonBody) {
        StringBuilder curl = new StringBuilder("curl -sS -X POST '").append(url).append("'");
        curl.append(" -H 'Content-Type: application/json'");
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && !name.isBlank() && value != null && !value.isBlank()) {
                    curl.append(" -H '").append(escapeShell(name)).append(": ")
                            .append(escapeShell(value)).append("'");
                }
            });
        }
        String correlation = org.slf4j.MDC.get(RequestInfoBuilder.CORRELATION_MDC);
        if (correlation != null && !correlation.isBlank()) {
            curl.append(" -H 'x-correlation-id: ").append(escapeShell(correlation)).append("'");
        }
        curl.append(" --data-raw '").append(escapeShell(jsonBody)).append("'");
        return curl.toString();
    }

    private static String formatBody(JsonNode body, ObjectMapper objectMapper) {
        try {
            if (body == null) {
                return "{}";
            }
            if (body instanceof ObjectNode objectNode) {
                JsonNode redacted = objectMapper.readTree(objectNode.toString());
                JsonNode info = redacted.get("RequestInfo");
                if (info instanceof ObjectNode infoNode && infoNode.has("authToken")) {
                    infoNode.put("authToken", maskToken(infoNode.get("authToken").asText()));
                }
                return objectMapper.writeValueAsString(redacted);
            }
            return objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            return String.valueOf(body);
        }
    }

    /** Full body for curl replay (local debugging). */
    static String bodyForCurl(JsonNode body, ObjectMapper objectMapper) {
        try {
            return body == null ? "{}" : objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            return "{}";
        }
    }

    public static void logPostWithReplayCurl(Logger log, String label, String baseUrl, String path,
                                             Map<String, List<String>> query, JsonNode body,
                                             Map<String, String> headers, ObjectMapper objectMapper) {
        String url = buildUrl(baseUrl, path, query);
        String redactedPayload = formatBody(body, objectMapper);
        String replayBody = bodyForCurl(body, objectMapper);
        String curl = toCurl(url, headers, replayBody);
        log.info("{} url={} payload={} curl={}", label, url, redactedPayload, curl);
    }

    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "[REDACTED]";
        }
        return token.substring(0, 8) + "…";
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String escapeShell(String value) {
        return value.replace("'", "'\"'\"'");
    }
}
