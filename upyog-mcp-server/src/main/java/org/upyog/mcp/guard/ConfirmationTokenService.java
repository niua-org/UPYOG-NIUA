package org.upyog.mcp.guard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.upyog.mcp.config.McpProperties;
import org.upyog.mcp.web.AuthenticatedUser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * HMAC-SHA256 confirmation token. The signed claims bind the user id, service, operation,
 * tenant, exact business-body hash, expiry, and a single-use id ({@code jti}).
 * The access token is not stored in the claims.
 */
@Component
public class ConfirmationTokenService {

    private final ObjectMapper objectMapper;
    private final McpProperties properties;

    public ConfirmationTokenService(ObjectMapper objectMapper, McpProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Signs a confirmation token binding user, service, operation, tenant, payload hash, expiry, and {@code jti}.
     *
     * @param businessBody gateway-ready body produced by {@link org.upyog.mcp.registry.BodyBuilder}; stored in claims
     */
    public Issued issue(AuthenticatedUser user, String service, String operation, String tenant, ObjectNode businessBody) {
        try {
            String payloadHash = sha256(canonical(businessBody));
            long expiresAt = Instant.now().plus(properties.getConfirmationTtl()).getEpochSecond();
            ObjectNode claims = objectMapper.createObjectNode();
            claims.put("userId", user.getUuid());
            claims.put("service", service);
            claims.put("operation", operation);
            claims.put("tenant", tenant);
            claims.put("payloadHash", payloadHash);
            claims.set("body", businessBody);
            claims.put("exp", expiresAt);
            claims.put("jti", UUID.randomUUID().toString());
            String encoded = base64(objectMapper.writeValueAsBytes(claims));
            String token = encoded + "." + base64(hmac(encoded));
            return new Issued(token, Instant.ofEpochSecond(expiresAt), payloadHash, claims.get("jti").asText(), businessBody);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign confirmation token", exception);
        }
    }

    /**
     * Verifies signature, expiry, user binding, and payload hash integrity.
     *
     * @return decoded claims including the stored {@code body} node
     * @throws IllegalArgumentException when the token is invalid, expired, or tampered
     */
    public ObjectNode verify(String token, AuthenticatedUser user) {
        int dot = token == null ? -1 : token.lastIndexOf('.');
        if (dot <= 0) {
            throw new IllegalArgumentException("confirmation token is invalid");
        }
        String encoded = token.substring(0, dot);
        String signature = token.substring(dot + 1);
        if (!constantTimeEquals(base64(hmac(encoded)), signature)) {
            throw new IllegalArgumentException("confirmation token is invalid");
        }
        try {
            ObjectNode claims = (ObjectNode) objectMapper.readTree(Base64.getUrlDecoder().decode(encoded));
            if (claims.path("exp").asLong() < Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("confirmation token has expired");
            }
            if (!user.getUuid().equals(claims.path("userId").asText())) {
                throw new IllegalArgumentException("confirmation token belongs to another user");
            }
            String bodyHash = sha256(canonical((ObjectNode) claims.get("body")));
            if (!bodyHash.equals(claims.path("payloadHash").asText())) {
                throw new IllegalArgumentException("confirmation token payload was modified");
            }
            return claims;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("confirmation token is invalid");
        }
    }

    private byte[] hmac(String encoded) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(encoded.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC failed", exception);
        }
    }

    private String canonical(ObjectNode body) throws Exception {
        return objectMapper.copy().configure(
                com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .writeValueAsString(body);
    }

    private static String sha256(String value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param token       opaque value returned to the assistant as {@code confirmationToken}
     * @param expiresAt   UTC instant after which confirm must be rejected
     * @param payloadHash canonical SHA-256 of the business body
     * @param jti         single-use id consumed in Redis on confirm
     * @param body        exact downstream JSON body to post on confirm
     */
    public record Issued(String token, Instant expiresAt, String payloadHash, String jti, ObjectNode body) {
    }
}
