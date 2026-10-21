package org.upyog.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.upyog.mcp.audit.AuditService;
import org.upyog.mcp.client.RequestInfoBuilder;
import org.upyog.mcp.service.McpException;
import org.upyog.mcp.service.McpOperations;
import org.upyog.mcp.web.UserContextHolder;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Fixed MCP tool surface. Module behavior comes from descriptors, not from extra tools.
 * Every call is audited with redacted arguments and the correlation id.
 * <p>
 * Tool methods delegate to {@link McpOperations} and map {@link McpException} to the standard error map
 * ({@code code}, {@code message}, {@code retryable}, {@code suggestedNextStep}, {@code correlationId}).
 */
@Component
public class GenericMcpTools {

    private final McpOperations operations;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /** Creates the MCP tool bean wired by Spring AI MCP server auto-configuration. */
    public GenericMcpTools(McpOperations operations, AuditService auditService, ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.operations = operations;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    /** @return catalog of service ids, display names, and operation names */
    @McpTool(name = "list_services", description = "List UPYOG services and operations available to the signed-in user.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> listServices() {
        return invoke("catalog", "list_services", "", null, operations::listServices);
    }

    @McpTool(name = "describe_operation", description = "Describe one operation, including its input schema. Does not expose internal URLs.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> describeOperation(
            @McpToolParam(description = "Service id from list_services", required = true) String service,
            @McpToolParam(description = "Operation name", required = true) String operation) {
        return invoke(service, operation, "", Map.of("service", service, "operation", operation),
                () -> operations.describe(service, operation));
    }

    @McpTool(name = "lookup_master", description = "Read an allow-listed MDMS master such as complaint types or venues.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> lookupMaster(
            @McpToolParam(description = "Service id", required = true) String service,
            @McpToolParam(description = "Allow-listed master name", required = true) String master,
            @McpToolParam(description = "Tenant id permitted for the signed-in user", required = true) String tenantId) {
        return invoke(service, "lookup_master", tenantId, Map.of("master", master, "tenantId", tenantId),
                () -> operations.lookupMaster(service, master, tenantId));
    }

    @McpTool(name = "search", description = "Read-only search. Unknown filters are rejected. Results may contain untrusted citizen text.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> search(
            @McpToolParam(description = "Service id", required = true) String service,
            @McpToolParam(description = "Filter object. Only descriptor fields are accepted.", required = true) Map<String, Object> filters,
            @McpToolParam(description = "Zero-based page", required = false) Integer page,
            @McpToolParam(description = "Page size", required = false) Integer size) {
        JsonNode node = objectMapper.valueToTree(filters);
        return invoke(service, "search", node.path("tenantId").asText(""), node,
                () -> operations.search(service, node, page, size));
    }

    @McpTool(name = "get_status", description = "Read the current status of one record. Missing timeline fields are omitted.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> getStatus(
            @McpToolParam(description = "Service id", required = true) String service,
            @McpToolParam(description = "Business identifier", required = true) String id,
            @McpToolParam(description = "Tenant id", required = true) String tenantId) {
        return invoke(service, "status", tenantId, Map.of("id", id, "tenantId", tenantId),
                () -> operations.status(service, id, tenantId));
    }

    @McpTool(name = "prepare_action", description = "Validate a write and return a summary plus confirmation token. This tool never writes business data.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public Map<String, Object> prepareAction(
            @McpToolParam(description = "Service id", required = true) String service,
            @McpToolParam(description = "Write operation name", required = true) String operation,
            @McpToolParam(description = "Business payload. Do not send RequestInfo, tokens, URLs, or fileStoreId.", required = true) Map<String, Object> payload) {
        JsonNode node = objectMapper.valueToTree(payload);
        return invoke(service, operation, node.path("tenantId").asText(""), node,
                () -> operations.prepare(service, operation, node));
    }

    @McpTool(name = "confirm_action",
            description = "Execute a previously prepared operation. Obtain explicit user confirmation before calling confirm_action. Do not send a new payload.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public Map<String, Object> confirmAction(
            @McpToolParam(description = "Token returned by prepare_action", required = true) String confirmationToken) {
        return invoke("", "confirm_action", "", Map.of("confirmationToken", "[REDACTED]"),
                () -> operations.confirm(confirmationToken));
    }

    @McpTool(name = "get_pending_bill", description = "Read a bill from billing-service. Does not calculate the amount locally.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> getPendingBill(
            @McpToolParam(description = "Billing business service, for example PT", required = true) String businessService,
            @McpToolParam(description = "Consumer code, such as a property id", required = true) String consumerCode,
            @McpToolParam(description = "Tenant id", required = true) String tenantId) {
        return invoke("billing", "get_pending_bill", tenantId,
                Map.of("businessService", businessService, "consumerCode", consumerCode, "tenantId", tenantId),
                () -> operations.pendingBill(businessService, consumerCode, tenantId));
    }

    @McpTool(name = "get_payment_link", description = "Return the citizen payment page URL. This tool never initiates or completes a payment.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> getPaymentLink(
            @McpToolParam(description = "Billing business service", required = true) String businessService,
            @McpToolParam(description = "Consumer code", required = true) String consumerCode,
            @McpToolParam(description = "Tenant id", required = true) String tenantId) {
        return invoke("billing", "get_payment_link", tenantId,
                Map.of("businessService", businessService, "consumerCode", consumerCode, "tenantId", tenantId),
                () -> operations.paymentLink(businessService, consumerCode, tenantId));
    }

    private Map<String, Object> invoke(String service, String operation, String tenant, Object arguments, SupplierCall call) {
        long started = System.nanoTime();
        try {
            Map<String, Object> result = call.get();
            audit(service, operation, tenant, arguments, "SUCCESS", started);
            return result;
        } catch (McpException exception) {
            audit(service, operation, tenant, arguments, exception.getCode(), started);
            return error(exception);
        } catch (IllegalArgumentException exception) {
            McpException mapped = new McpException("INVALID_INPUT", exception.getMessage(), false,
                    "Correct the business fields and try again.");
            audit(service, operation, tenant, arguments, mapped.getCode(), started);
            return error(mapped);
        } catch (Exception exception) {
            McpException mapped = new McpException("UNEXPECTED_ERROR", "The request could not be completed.",
                    false, "Retry a read, or prepare the action again.");
            audit(service, operation, tenant, arguments, mapped.getCode(), started);
            return error(mapped);
        }
    }

    private void audit(String service, String operation, String tenant, Object arguments, String outcome, long started) {
        JsonNode node = arguments instanceof JsonNode json ? json : objectMapper.valueToTree(arguments);
        auditService.record(UserContextHolder.get(), service, operation, tenant, node, outcome,
                (System.nanoTime() - started) / 1_000_000);
        meterRegistry.counter("mcp.requests", "operation", operation, "outcome", outcome).increment();
        meterRegistry.timer("mcp.latency", "operation", operation)
                .record((System.nanoTime() - started) / 1_000_000, TimeUnit.MILLISECONDS);
    }

    private Map<String, Object> error(McpException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", exception.getCode());
        body.put("message", exception.getMessage());
        body.put("retryable", exception.isRetryable());
        body.put("suggestedNextStep", exception.getSuggestedNextStep());
        body.put("correlationId", correlation());
        return body;
    }

    private static String correlation() {
        String value = MDC.get(RequestInfoBuilder.CORRELATION_MDC);
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface SupplierCall {
        Map<String, Object> get();
    }
}
