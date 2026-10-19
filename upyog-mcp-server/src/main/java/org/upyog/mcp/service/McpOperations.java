package org.upyog.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.upyog.mcp.client.GatewayClient;
import org.upyog.mcp.client.RequestInfoBuilder;
import org.upyog.mcp.config.McpProperties;
import org.upyog.mcp.guard.ConfirmationTokenService;
import org.upyog.mcp.guard.PayloadGuard;
import org.upyog.mcp.guard.SingleUseStore;
import org.upyog.mcp.guard.TenantValidator;
import org.upyog.mcp.mapper.PiiMasker;
import org.upyog.mcp.mapper.ResponseProjector;
import org.upyog.mcp.mapper.SchemaValidator;
import org.upyog.mcp.registry.BodyBuilder;
import org.upyog.mcp.registry.DescriptorRegistry;
import org.upyog.mcp.registry.ServiceDescriptor;
import org.upyog.mcp.web.AuthenticatedUser;
import org.upyog.mcp.web.UserContextHolder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates descriptor-backed tool calls.
 * <p>
 * {@link #prepare} validates and returns a confirmation token. It does not call a business write.
 * {@link #confirm} sends only the body stored in that token, after Redis marks the token used.
 */
@Service
public class McpOperations {

    private final DescriptorRegistry registry;
    private final GatewayClient gatewayClient;
    private final RequestInfoBuilder requestInfoBuilder;
    private final BodyBuilder bodyBuilder;
    private final SchemaValidator schemaValidator;
    private final ResponseProjector responseProjector;
    private final PiiMasker piiMasker;
    private final TenantValidator tenantValidator;
    private final ConfirmationTokenService confirmationTokenService;
    private final SingleUseStore singleUseStore;
    private final McpProperties properties;
    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;

    /**
     * Spring-injected orchestrator; all MCP business logic flows through this bean.
     */
    public McpOperations(DescriptorRegistry registry, GatewayClient gatewayClient, RequestInfoBuilder requestInfoBuilder,
                         BodyBuilder bodyBuilder, SchemaValidator schemaValidator, ResponseProjector responseProjector,
                         PiiMasker piiMasker, TenantValidator tenantValidator, ConfirmationTokenService confirmationTokenService,
                         SingleUseStore singleUseStore, McpProperties properties, ObjectMapper objectMapper,
                         RateLimitService rateLimitService) {
        this.registry = registry;
        this.gatewayClient = gatewayClient;
        this.requestInfoBuilder = requestInfoBuilder;
        this.bodyBuilder = bodyBuilder;
        this.schemaValidator = schemaValidator;
        this.responseProjector = responseProjector;
        this.piiMasker = piiMasker;
        this.tenantValidator = tenantValidator;
        this.confirmationTokenService = confirmationTokenService;
        this.singleUseStore = singleUseStore;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.rateLimitService = rateLimitService;
    }

    /** @return services and operation names visible to the current user's roles */
    public Map<String, Object> listServices() {
        AuthenticatedUser user = currentUser();
        List<Map<String, Object>> services = new ArrayList<>();
        for (ServiceDescriptor service : registry.all()) {
            if (!visible(user, service)) {
                continue;
            }
            List<String> operations = new ArrayList<>(service.getOperations().keySet());
            services.add(Map.of(
                    "id", service.getId(),
                    "displayName", service.getDisplayName(),
                    "operations", operations
            ));
        }
        return Map.of("services", services, "correlationId", RequestInfoBuilder.correlationId());
    }

    /** @return input schema and hints for one operation; gateway paths are not exposed */
    public Map<String, Object> describe(String serviceId, String operationName) {
        currentUser();
        ServiceDescriptor.OperationDescriptor operation = operation(serviceId, operationName);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", serviceId);
        body.put("operation", operationName);
        body.put("description", operation.getDescription());
        body.put("inputSchema", operation.getInputSchema());
        body.put("required", operation.getInputSchema().path("required"));
        body.put("readOnly", operation.isReadOnly());
        body.put("destructive", operation.isDestructive());
        body.put("requiresConfirmation", operation.isRequiresConfirmation());
        body.put("examples", operation.getExamples());
        body.put("correlationId", RequestInfoBuilder.correlationId());
        return body;
    }

    /** @return allow-listed MDMS master rows with PII masked and {@code untrustedData: true} */
    public Map<String, Object> lookupMaster(String serviceId, String masterName, String tenantId) {
        AuthenticatedUser user = currentUser();
        tenantValidator.requireAllowed(user, tenantId);
        ServiceDescriptor service = registry.require(serviceId);
        ServiceDescriptor.MasterRef master = service.getMasters().stream()
                .filter(item -> item.getName().equals(masterName))
                .findFirst()
                .orElseThrow(() -> new McpException("MASTER_NOT_ALLOWED", "That master is not allow-listed.",
                        false, "Use a master returned by describe_operation or list_services."));
        ObjectNode criteria = objectMapper.createObjectNode();
        criteria.put("tenantId", tenantId);
        ArrayNode moduleDetails = criteria.putArray("moduleDetails");
        ObjectNode module = moduleDetails.addObject();
        module.put("moduleName", master.getModule());
        ArrayNode masterDetails = module.putArray("masterDetails");
        masterDetails.addObject().put("name", master.getName());
        ObjectNode body = requestInfoBuilder.wrap(user, objectMapper.createObjectNode().set("MdmsCriteria", criteria));
        JsonNode response = gatewayClient.postJson("/egov-mdms-service/v1/_search", Map.of(), body, true);
        JsonNode data = response.path("MdmsRes");
        return Map.of(
                "master", masterName,
                "data", piiMasker.mask(data.deepCopy(), false),
                "untrustedData", true,
                "correlationId", RequestInfoBuilder.correlationId()
        );
    }

    /**
     * Runs a descriptor read search with validated filters and pagination caps.
     *
     * @return projected {@code items}, optional {@code count}, {@code untrustedData}, and {@code correlationId}
     */
    public Map<String, Object> search(String serviceId, JsonNode filters, Integer page, Integer size) {
        AuthenticatedUser user = currentUser();
        ServiceDescriptor.OperationDescriptor operation = readOperation(serviceId, "search");
        JsonNode payload = validatePayload(user, operation, filters);
        int limit = size == null ? properties.getMaxPageSize() : Math.min(size, properties.getMaxPageSize());
        int offset = Math.max(page == null ? 0 : page, 0) * limit;
        Map<String, List<String>> query = query(operation, payload, user, limit, offset);
        ObjectNode body = requestInfoBuilder.wrap(user, objectMapper.createObjectNode());
        JsonNode response = gatewayClient.postJson(operation.getGatewayPath(), query, body, true);
        ObjectNode projected = responseProjector.project(response, operation.getResponse());
        projected.put("correlationId", RequestInfoBuilder.correlationId());
        return objectMapper.convertValue(projected, Map.class);
    }

    /** Delegates to search using the descriptor's status argument (for example {@code serviceRequestId}). */
    public Map<String, Object> status(String serviceId, String id, String tenantId) {
        AuthenticatedUser user = currentUser();
        ServiceDescriptor service = registry.require(serviceId);
        ServiceDescriptor.OperationDescriptor operation = service.getOperations().values().stream()
                .filter(ServiceDescriptor.OperationDescriptor::isSupportsStatus)
                .findFirst()
                .orElseThrow(() -> new McpException("STATUS_UNAVAILABLE", "This service has no status operation.",
                        false, "Use search instead."));
        ObjectNode filters = objectMapper.createObjectNode();
        filters.put("tenantId", tenantId);
        if (operation.isStatusArgumentAsList()) {
            filters.putArray(operation.getStatusArgument()).add(id);
        } else {
            filters.put(operation.getStatusArgument(), id);
        }
        Map<String, Object> found = search(serviceId, filters, 0, 5);
        return found;
    }

    /**
     * Validates a write and returns a summary plus confirmation token.
     * Business create and update URLs are not called from this method.
     * File bytes, when present, are uploaded to filestore and replaced with {@code fileStoreId}.
     */
    public Map<String, Object> prepare(String serviceId, String operationName, JsonNode payload) {
        AuthenticatedUser user = currentUser();
        ServiceDescriptor.OperationDescriptor operation = operation(serviceId, operationName);
        if (!operation.isRequiresConfirmation()) {
            throw new McpException("NOT_A_WRITE", "This operation does not use confirmation.",
                    false, "Use search, get_status, or lookup_master.");
        }
        JsonNode valid = validatePayload(user, operation, payload);
        ObjectNode business = bodyBuilder.build(operation.getRequestBody(), valid, user, 0, 0);
        attachDocuments(user, operation, valid, business);
        String tenant = valid.path("tenantId").asText();
        ConfirmationTokenService.Issued issued = confirmationTokenService.issue(user, serviceId, operationName, tenant, business);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("readyForConfirmation", true);
        result.put("requiresConfirmation", true);
        result.put("summary", summary(serviceId, operationName, business));
        result.put("confirmationToken", issued.token());
        result.put("expiresAt", issued.expiresAt().toString());
        result.put("correlationId", RequestInfoBuilder.correlationId());
        result.put("instruction", "Obtain explicit user confirmation before calling confirm_action.");
        return result;
    }

    /**
     * Executes the body stored in a previously issued confirmation token.
     * The caller cannot send a new payload. Redis must accept the token id exactly once.
     */
    public Map<String, Object> confirm(String confirmationToken) {
        AuthenticatedUser user = currentUser();
        if (!singleUseStore.writesEnabled()) {
            throw new McpException("CONFIRMATION_STORE_UNAVAILABLE",
                    "Writes are disabled until Redis is configured for single-use tokens.",
                    false, "Enable Redis and prepare the action again.");
        }
        ObjectNode claims = confirmationTokenService.verify(confirmationToken, user);
        tenantValidator.requireAllowed(user, claims.path("tenant").asText());
        if (!singleUseStore.consumeOnce("confirm:" + claims.path("jti").asText(), properties.getConfirmationTtl())) {
            throw new McpException("TOKEN_ALREADY_USED", "This confirmation token was already used.",
                    false, "Prepare the action again and confirm once.");
        }
        String idemKey = "idem:" + claims.path("userId").asText() + ":" + claims.path("payloadHash").asText();
        var existing = singleUseStore.findResult(idemKey);
        if (existing.isPresent()) {
            return readStored(existing.get());
        }
        ServiceDescriptor.OperationDescriptor operation = operation(claims.path("service").asText(), claims.path("operation").asText());
        ObjectNode business = (ObjectNode) claims.get("body");
        ObjectNode request = requestInfoBuilder.wrap(user, business);
        JsonNode response = gatewayClient.postJson(operation.getGatewayPath(), Map.of(), request, false);
        ObjectNode projected = responseProjector.project(response, operation.getResponse());
        projected.put("correlationId", RequestInfoBuilder.correlationId());
        try {
            singleUseStore.saveResult(idemKey, objectMapper.writeValueAsString(projected), Duration.ofHours(24));
        } catch (Exception exception) {
            return objectMapper.convertValue(projected, Map.class);
        }
        return objectMapper.convertValue(projected, Map.class);
    }

    /**
     * Calls billing-service {@code /bill/v2/_fetchbill} and returns that JSON.
     * {@code hasPendingBill} is true only when the {@code Bill} array is non-empty.
     * Finer paid/due rules wait for a captured fetch-bill sample.
     */
    public Map<String, Object> pendingBill(String businessService, String consumerCode, String tenantId) {
        AuthenticatedUser user = currentUser();
        tenantValidator.requireAllowed(user, tenantId);
        ObjectNode body = requestInfoBuilder.wrap(user, objectMapper.createObjectNode());
        JsonNode response = gatewayClient.postJson("/billing-service/bill/v2/_fetchbill", Map.of(
                "tenantId", List.of(tenantId),
                "businessService", List.of(businessService),
                "consumerCode", List.of(consumerCode)
        ), body, true);
        JsonNode bills = response.path("Bill");
        boolean pending = bills.isArray() && !bills.isEmpty();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasPendingBill", pending);
        result.put("pendingRule", "non-empty Bill array; amount rules wait for the fetch-bill sample");
        result.put("billingResponse", piiMasker.mask(response.deepCopy(), true));
        result.put("correlationId", RequestInfoBuilder.correlationId());
        return result;
    }

    /**
     * Builds the citizen payment-page URL after a payable bill is confirmed.
     * The method does not call collection-services and does not take payment.
     */
    public Map<String, Object> paymentLink(String businessService, String consumerCode, String tenantId) {
        Map<String, Object> bill = pendingBill(businessService, consumerCode, tenantId);
        if (!Boolean.TRUE.equals(bill.get("hasPendingBill"))) {
            return Map.of(
                    "hasPendingBill", false,
                    "correlationId", RequestInfoBuilder.correlationId()
            );
        }
        String url = trimSlash(properties.getUiBaseUrl())
                + "/upyog-ui/citizen/payment/my-bills/"
                + encode(businessService) + "/" + encode(consumerCode)
                + "?tenantId=" + encode(tenantId);
        return Map.of(
                "hasPendingBill", true,
                "paymentUrl", url,
                "initiatesPayment", false,
                "correlationId", RequestInfoBuilder.correlationId()
        );
    }

    private void attachDocuments(AuthenticatedUser user, ServiceDescriptor.OperationDescriptor operation,
                                 JsonNode payload, ObjectNode business) {
        JsonNode documents = payload.get("documents");
        if (documents == null || documents.isNull() || documents.isEmpty()) {
            return;
        }
        if (operation.getFilestoreModule() == null || operation.getDocumentsTarget() == null) {
            throw new McpException("DOCUMENTS_NOT_ACCEPTED", "This operation does not accept documents.",
                    false, "Remove the documents and prepare again.");
        }
        ArrayNode stored = objectMapper.createArrayNode();
        for (JsonNode document : documents) {
            byte[] bytes = Base64.getDecoder().decode(document.path("contentBase64").asText(""));
            if (bytes.length == 0 || bytes.length > properties.getMaxUploadBytes()) {
                throw new McpException("FILE_TOO_LARGE", "The document exceeds the upload limit.",
                        false, "Use a smaller file or continue in the UPYOG UI.");
            }
            JsonNode uploaded = gatewayClient.postFile("/filestore/v1/files", payload.path("tenantId").asText(),
                    operation.getFilestoreModule(), document.path("fileName").asText("document"),
                    document.path("contentType").asText("application/octet-stream"), bytes);
            String fileStoreId = uploaded.path("files").path(0).path("fileStoreId").asText("");
            if (fileStoreId.isBlank()) {
                throw new McpException("FILE_UPLOAD_FAILED", "The document store did not return an id.",
                        true, "Try the upload again.");
            }
            ObjectNode item = stored.addObject();
            item.put("documentType", document.path("documentType").asText(""));
            item.put("fileStoreId", fileStoreId);
        }
        putAt(business, operation.getDocumentsTarget(), stored);
    }

    private void putAt(ObjectNode root, String dotted, JsonNode value) {
        String[] parts = dotted.split("\\.");
        ObjectNode current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonNode next = current.get(parts[i]);
            if (!(next instanceof ObjectNode)) {
                next = objectMapper.createObjectNode();
                current.set(parts[i], next);
            }
            current = (ObjectNode) next;
        }
        current.set(parts[parts.length - 1], value);
    }

    private Map<String, List<String>> query(ServiceDescriptor.OperationDescriptor operation, JsonNode payload,
                                            AuthenticatedUser user, int limit, int offset) {
        ObjectNode resolved = bodyBuilder.build(operation.getQuery(), payload, user, limit, offset);
        Map<String, List<String>> query = new LinkedHashMap<>();
        resolved.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isArray()) {
                List<String> values = new ArrayList<>();
                value.forEach(item -> values.add(item.asText()));
                query.put(entry.getKey(), values);
            } else if (!value.isNull() && !value.asText().isBlank()) {
                query.put(entry.getKey(), List.of(value.asText()));
            }
        });
        return query;
    }

    private JsonNode validatePayload(AuthenticatedUser user, ServiceDescriptor.OperationDescriptor operation, JsonNode payload) {
        JsonNode body = payload == null || payload.isNull() ? objectMapper.createObjectNode() : payload;
        try {
            PayloadGuard.rejectForbidden(body);
            schemaValidator.validate(operation.getInputSchema(), body);
        } catch (IllegalArgumentException exception) {
            throw new McpException("INVALID_INPUT", exception.getMessage(), false,
                    "Use describe_operation and send only the listed fields.");
        }
        tenantValidator.requireAllowed(user, body.path("tenantId").asText(null));
        return body;
    }

    private ServiceDescriptor.OperationDescriptor readOperation(String serviceId, String name) {
        ServiceDescriptor.OperationDescriptor operation = operation(serviceId, name);
        if (!operation.isReadOnly()) {
            throw new McpException("NOT_A_READ", "This tool only runs read operations.",
                    false, "Use prepare_action for writes.");
        }
        return operation;
    }

    private ServiceDescriptor.OperationDescriptor operation(String serviceId, String name) {
        ServiceDescriptor service = registry.find(serviceId).orElseThrow(() ->
                new McpException("UNKNOWN_SERVICE", "Unknown service.", false, "Call list_services."));
        ServiceDescriptor.OperationDescriptor operation = service.getOperations().get(name);
        if (operation == null) {
            throw new McpException("UNKNOWN_OPERATION", "Unknown operation.", false, "Call describe_operation.");
        }
        return operation;
    }

    private boolean visible(AuthenticatedUser user, ServiceDescriptor service) {
        if (user.hasRole("SUPERUSER") || service.getAllowedRolesHint().isEmpty()) {
            return true;
        }
        return service.getAllowedRolesHint().stream().anyMatch(user::hasRole);
    }

    private AuthenticatedUser currentUser() {
        AuthenticatedUser user = UserContextHolder.get();
        if (user == null) {
            throw new McpException("AUTHENTICATION_REQUIRED", "An auth-token header is required.",
                    false, "Pass the UPYOG session token in the auth-token header.");
        }
        rateLimitService.acquire(user.getUuid());
        return user;
    }

    private String summary(String service, String operation, ObjectNode business) {
        JsonNode masked = piiMasker.mask(business.deepCopy(), true);
        return "This will call the UPYOG " + service + " " + operation
                + " operation through the API gateway. Business details: " + masked
                + ". No write has been executed yet.";
    }

    private Map<String, Object> readStored(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception exception) {
            throw new McpException("STORED_RESULT_UNREADABLE", "The previous result could not be read.",
                    false, "Prepare the action again.");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String trimSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
