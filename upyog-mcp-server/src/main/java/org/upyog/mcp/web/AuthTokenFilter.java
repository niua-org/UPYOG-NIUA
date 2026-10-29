package org.upyog.mcp.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.mcp.client.RequestInfoBuilder;
import org.upyog.mcp.client.UserSessionClient;
import org.upyog.mcp.config.McpProperties;
import org.upyog.mcp.service.McpException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * Resolves the caller on {@code /mcp}.
 * <p>
 * Production path: chatbot → API gateway (auth + RBAC) → MCP, with {@code x-user-info} and {@code Auth-Token}.
 * Developer path: chatbot → MCP directly with {@code auth-token}; MCP validates via {@code POST /user/_search}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenFilter.class);

    /** HTTP header name for the UPYOG session access token on direct MCP calls. */
    public static final String HEADER = "auth-token";

    private final UserSessionClient userSessionClient;
    private final ObjectMapper objectMapper;
    private final McpProperties properties;
    private final Cache<String, AuthenticatedUser> users = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    /**
     * @param objectMapper  JSON serializer for error responses
     */
    public AuthTokenFilter(UserSessionClient userSessionClient, ObjectMapper objectMapper, McpProperties properties) {
        this.userSessionClient = userSessionClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** Applies only to {@code /mcp} routes; health and actuator paths skip token validation. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/mcp");
    }

    /** Request attribute so async MCP dispatch can re-bind identity after {@link UserContextHolder#clear()}. */
    static final String AUTHENTICATED_USER_ATTR = AuthTokenFilter.class.getName() + ".user";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            Object cached = request.getAttribute(AUTHENTICATED_USER_ATTR);
            if (cached instanceof AuthenticatedUser asyncUser) {
                UserContextHolder.set(asyncUser);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    UserContextHolder.clear();
                }
                return;
            }
        }
        AuthenticatedUser user;
        try {
            user = resolveUser(request);
        } catch (Exception exception) {
            Throwable root = unwrap(exception);
            if (root instanceof McpException mcp) {
                log.warn("mcp_session_check_failed correlationId={} code={} reason={}",
                        RequestInfoBuilder.correlationId(), mcp.getCode(), mcp.getMessage());
                writeMcpFailure(response, mcp);
                return;
            }
            log.warn("mcp_auth_failed correlationId={} reason={}",
                    RequestInfoBuilder.correlationId(), root.getMessage(), root);
            write(response, 401, "AUTHENTICATION_FAILED", "The auth token could not be validated.",
                    safeReason(root), false, "Sign in to UPYOG and pass the session auth-token header.");
            return;
        }
        if (user == null) {
            write(response, 401, "AUTHENTICATION_REQUIRED",
                    "Call MCP through the API gateway with Auth-Token, or pass auth-token for local direct access.",
                    null, false,
                    "Sign in to UPYOG and pass the session auth-token header.");
            return;
        }
        request.setAttribute(AUTHENTICATED_USER_ATTR, user);
        UserContextHolder.set(user);
        log.info("mcp_auth_succeeded correlationId={} uuid={}", RequestInfoBuilder.correlationId(), user.getUuid());
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    private AuthenticatedUser resolveUser(HttpServletRequest request) {
        // Production path: gateway has already validated auth — trust its headers directly.
        if (properties.isTrustGatewayIdentity()) {
            // Prefer x-pass-through-gateway + x-user-info (full gateway-enriched identity).
            AuthenticatedUser fromGateway = GatewayIdentityParser.fromGatewayHeaders(request, objectMapper);
            if (fromGateway != null) {
                return fromGateway;
            }
            // Chatbot sends auth-token + x-user-uuid (no gateway pass-through headers).
            // Build identity directly — no upstream call needed; gateway validates the token
            // on every downstream service call via RequestInfo.authToken.
            String token = UserSessionClient.normalizeToken(GatewayIdentityParser.firstToken(request));
            if (token.isBlank()) {
                return null;
            }
            String userUuid = UserUuidResolver.fromRequest(request, objectMapper);
            com.fasterxml.jackson.databind.JsonNode userInfo = UserUuidResolver.userInfoFromRequest(request, objectMapper);
            if (!userUuid.isBlank()) {
                return buildFromHeaders(token, userUuid, userInfo);
            }
            return null;
        }
        // Local dev path (trust-gateway-identity=false): validate token via /user/_search.
        String token = UserSessionClient.normalizeToken(GatewayIdentityParser.firstToken(request));
        if (token.isBlank()) {
            return null;
        }
        String userUuid = UserUuidResolver.fromRequest(request, objectMapper);
        com.fasterxml.jackson.databind.JsonNode userInfo = UserUuidResolver.userInfoFromRequest(request, objectMapper);
        String cacheKey = hash(token) + ":" + userUuid;
        return users.get(cacheKey, key -> userSessionClient.resolve(token, userUuid, userInfo));
    }

    /**
     * Builds {@link AuthenticatedUser} directly from request headers when the gateway is trusted.
     * Uses full {@code x-user-info} when present; falls back to a minimal CITIZEN stub from uuid.
     */
    private AuthenticatedUser buildFromHeaders(String token, String uuid,
                                               com.fasterxml.jackson.databind.JsonNode userInfo) {
        if (userInfo != null && !userInfo.isNull() && !userInfo.path("uuid").asText("").isBlank()) {
            java.util.List<String> roles = new java.util.ArrayList<>();
            java.util.List<String> roleTenants = new java.util.ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode role : userInfo.path("roles")) {
                roles.add(role.path("code").asText(""));
                roleTenants.add(role.path("tenantId").asText(""));
            }
            String tenantId = userInfo.path("tenantId").asText("");
            if (tenantId.isBlank()) tenantId = properties.getUserSessionTenantId();
            String type = userInfo.path("type").asText("CITIZEN");
            return new AuthenticatedUser(uuid, type, tenantId, roles, roleTenants, token);
        }
        // Minimal CITIZEN stub — downstream service calls carry authToken in RequestInfo;
        // the gateway validates and enriches userInfo on those calls.
        String tenantId = properties.getUserSessionTenantId();
        log.info("mcp_identity_stub uuid={} tenantId={}", uuid, tenantId);
        return new AuthenticatedUser(uuid, "CITIZEN", tenantId,
                java.util.List.of("CITIZEN"), java.util.List.of(tenantId), token);
    }

    private void writeMcpFailure(HttpServletResponse response, McpException mcp) throws IOException {
        int status = switch (mcp.getCode()) {
            case "DOWNSTREAM_UNAVAILABLE", "CIRCUIT_OPEN" -> 503;
            case "NOT_AUTHORIZED" -> 401;
            default -> 502;
        };
        String code = "NOT_AUTHORIZED".equals(mcp.getCode()) ? "AUTHENTICATION_FAILED" : mcp.getCode();
        String message = "NOT_AUTHORIZED".equals(mcp.getCode())
                ? "The auth token could not be validated."
                : mcp.getMessage();
        write(response, status, code, message, mcp.getMessage(), mcp.isRetryable(), mcp.getSuggestedNextStep());
    }

    private void write(HttpServletResponse response, int status, String code, String message, String reason,
                       boolean retryable, String suggestedNextStep) throws IOException {
        if (response.isCommitted()) {
            log.warn("mcp_auth_response_skipped responseAlreadyCommitted code={}", code);
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("retryable", retryable);
        body.put("suggestedNextStep", suggestedNextStep);
        body.put("correlationId", RequestInfoBuilder.correlationId());
        if (reason != null && !reason.isBlank()) {
            body.put("reason", reason);
        }
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static String safeReason(Throwable root) {
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return null;
        }
        if (message.length() > 240 || message.contains("Exception") || message.contains("http://")) {
            return null;
        }
        return message;
    }

    private static Throwable unwrap(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof McpException) {
                return current;
            }
            Throwable cause = current.getCause();
            if (cause == null) {
                break;
            }
            if (current instanceof java.util.concurrent.CompletionException
                    || current instanceof java.util.concurrent.ExecutionException
                    || cause instanceof McpException) {
                current = cause;
                continue;
            }
            break;
        }
        return current == null ? exception : current;
    }

    private static String hash(String token) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
