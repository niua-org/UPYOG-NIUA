package org.upyog.mcp.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Masks mobile numbers, email addresses, Aadhaar-like ids, and personal names
 * before a payload is returned to the assistant or written to the audit log.
 */
@Component
public class PiiMasker {

    private static final Set<String> MOBILE_KEYS = Set.of(
            "mobilenumber", "applicantmobileno", "applicantalternatemobileno");
    private static final Set<String> EMAIL_KEYS = Set.of("email", "emailid", "applicantemailid");
    private static final Set<String> ID_KEYS = Set.of("aadhaar", "aadhaarnumber", "aadharnumber");
    private static final Set<String> NAME_KEYS = Set.of("name", "applicantname", "ownername", "username");

    /**
     * Recursively masks known PII fields in place.
     *
     * @param maskNames when {@code true}, personal names are partially masked
     */
    public JsonNode mask(JsonNode node, boolean maskNames) {
        return walk(node, maskNames);
    }

    private JsonNode walk(JsonNode node, boolean maskNames) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(name -> {
                String key = name.toLowerCase();
                JsonNode child = objectNode.get(name);
                if (MOBILE_KEYS.contains(key)) {
                    objectNode.put(name, maskMobile(child.asText("")));
                } else if (EMAIL_KEYS.contains(key)) {
                    objectNode.put(name, maskEmail(child.asText("")));
                } else if (ID_KEYS.contains(key)) {
                    objectNode.put(name, "********");
                } else if (maskNames && NAME_KEYS.contains(key) && child.isTextual()) {
                    objectNode.put(name, maskName(child.asText()));
                } else {
                    objectNode.set(name, walk(child, maskNames));
                }
            });
            return objectNode;
        }
        if (node instanceof ArrayNode arrayNode) {
            for (int i = 0; i < arrayNode.size(); i++) {
                arrayNode.set(i, walk(arrayNode.get(i), maskNames));
            }
        }
        return node;
    }

    public static String maskMobile(String value) {
        if (value == null || value.length() < 4) {
            return "******";
        }
        return "******" + value.substring(value.length() - 4);
    }

    public static String maskEmail(String value) {
        int at = value == null ? -1 : value.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    public static String maskName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.charAt(0) + "***";
    }

    public static TextNode redacted() {
        return TextNode.valueOf("[REDACTED]");
    }
}
