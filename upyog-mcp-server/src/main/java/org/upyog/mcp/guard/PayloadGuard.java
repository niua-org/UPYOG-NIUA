package org.upyog.mcp.guard;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.Set;

/**
 * Rejects identity, transport, and file-store fields supplied by the assistant.
 * Document bytes may arrive as {@code contentBase64}. The server uploads them and
 * discards that field before the business body is stored in the confirmation token.
 */
public final class PayloadGuard {

    private static final Set<String> FORBIDDEN = Set.of(
            "requestinfo", "authtoken", "userinfo", "uuid", "roles", "url", "httpmethod",
            "method", "gatewaypath", "filestoreid"
    );

    private PayloadGuard() {
    }

    /**
     * Walks the payload tree and rejects keys that would let the assistant override identity or transport.
     *
     * @param node validated business payload from an MCP tool
     * @throws IllegalArgumentException when a forbidden field name appears at any depth
     */
    public static void rejectForbidden(JsonNode node) {
        walk(node);
    }

    private static void walk(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(name -> {
                if (FORBIDDEN.contains(name.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Field is not accepted: " + name);
                }
                walk(node.get(name));
            });
        } else if (node.isArray()) {
            node.forEach(PayloadGuard::walk);
        }
    }
}
