package org.upyog.mcp.registry;

import com.fasterxml.jackson.databind.JsonNode;
import org.upyog.mcp.config.McpProperties;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fails startup when a descriptor can select an arbitrary method, path, or template directive.
 * Allowed body directives are {@code $payload}, {@code $constant}, {@code $page}, and {@code $user}.
 */
public class DescriptorValidator {

    private static final Pattern PATH = Pattern.compile("^/[a-zA-Z0-9._/-]+$");
    private static final Set<String> TYPES = Set.of("READ", "WRITE");
    private static final Set<String> DIRECTIVES = Set.of("$payload", "$constant", "$page", "$user");

    private final McpProperties properties;

    public DescriptorValidator(McpProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates one service descriptor; throws {@link IllegalStateException} on any policy violation.
     */
    public void validate(ServiceDescriptor service) {
        require(service.getId(), "service id");
        require(service.getDisplayName(), "displayName");
        if (service.getOperations() == null || service.getOperations().isEmpty()) {
            fail(service.getId(), "at least one operation is required");
        }
        Set<String> names = new HashSet<>();
        for (ServiceDescriptor.OperationDescriptor operation : service.getOperations().values()) {
            if (operation.getName() == null || !names.add(operation.getName())) {
                fail(service.getId(), "duplicate or missing operation name");
            }
            if (!TYPES.contains(operation.getType())) {
                fail(service.getId(), "unsupported operation type " + operation.getType());
            }
            if (!"POST".equals(operation.getHttpMethod())) {
                fail(service.getId(), "only POST is allowed, found " + operation.getHttpMethod());
            }
            validatePath(service.getId(), operation.getGatewayPath());
            if (operation.getInputSchema() == null || !operation.getInputSchema().isObject()) {
                fail(service.getId(), operation.getName() + " is missing inputSchema");
            }
            validateSchema(service.getId(), operation.getName(), operation.getInputSchema());
            if (operation.getRequestBody() != null) {
                validateDirectives(service.getId(), operation.getRequestBody());
            }
            if (operation.getQuery() != null) {
                validateDirectives(service.getId(), operation.getQuery());
            }
            if ("WRITE".equals(operation.getType()) && !operation.isRequiresConfirmation()) {
                fail(service.getId(), operation.getName() + " writes must require confirmation");
            }
            if ("READ".equals(operation.getType()) && operation.isRequiresConfirmation()) {
                fail(service.getId(), operation.getName() + " reads must not require confirmation");
            }
        }
    }

    private void validatePath(String serviceId, String path) {
        if (path == null || !PATH.matcher(path).matches() || path.contains("..") || path.contains("://")) {
            fail(serviceId, "invalid gateway path");
        }
        boolean allowed = properties.getAllowedGatewayPrefixes().stream().anyMatch(path::startsWith);
        if (!allowed) {
            fail(serviceId, "gateway path is not on the allow list: " + path);
        }
    }

    private void validateSchema(String serviceId, String operation, JsonNode schema) {
        if (!schema.path("additionalProperties").isBoolean() || schema.path("additionalProperties").asBoolean()) {
            fail(serviceId, operation + " inputSchema must set additionalProperties false");
        }
        JsonNode properties = schema.get("properties");
        if (properties != null && properties.isObject()) {
            properties.fields().forEachRemaining(entry -> {
                if (entry.getValue().isObject() && "object".equals(entry.getValue().path("type").asText())) {
                    validateSchema(serviceId, operation + "." + entry.getKey(), entry.getValue());
                }
            });
        }
    }

    private void validateDirectives(String serviceId, JsonNode node) {
        if (node == null || node.isValueNode()) {
            return;
        }
        if (node.isObject()) {
            Set<String> keys = new HashSet<>();
            node.fieldNames().forEachRemaining(keys::add);
            boolean directive = keys.stream().anyMatch(key -> key.startsWith("$"));
            if (directive) {
                if (keys.size() != 1 || !DIRECTIVES.contains(keys.iterator().next())) {
                    fail(serviceId, "template directive must be exactly one of " + DIRECTIVES);
                }
                return;
            }
            node.elements().forEachRemaining(child -> validateDirectives(serviceId, child));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> validateDirectives(serviceId, child));
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Descriptor missing " + name);
        }
    }

    private static void fail(String serviceId, String message) {
        throw new IllegalStateException("Invalid descriptor [" + serviceId + "]: " + message);
    }

    /** Lowercases and trims a string for safe comparisons (for example forbidden field names). */
    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
