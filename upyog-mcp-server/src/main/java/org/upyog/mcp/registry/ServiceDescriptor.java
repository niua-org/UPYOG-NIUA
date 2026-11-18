package org.upyog.mcp.registry;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One UPYOG module loaded from {@code src/main/resources/descriptors}.
 * Adding a normal module is a new YAML file, not a new MCP tool.
 * <p>
 * Jackson maps YAML properties to the fields below; getters and setters exist for deserialization only.
 */
public class ServiceDescriptor {

    /** Stable service id passed as the MCP {@code service} argument ({@code pgr}, {@code property}, …). */
    private String id;
    private String displayName;
    private String description;
    private List<String> allowedRolesHint = new ArrayList<>();
    private List<MasterRef> masters = new ArrayList<>();
    private Map<String, OperationDescriptor> operations = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAllowedRolesHint() {
        return allowedRolesHint;
    }

    public void setAllowedRolesHint(List<String> allowedRolesHint) {
        this.allowedRolesHint = allowedRolesHint;
    }

    public List<MasterRef> getMasters() {
        return masters;
    }

    public void setMasters(List<MasterRef> masters) {
        this.masters = masters;
    }

    public Map<String, OperationDescriptor> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, OperationDescriptor> operations) {
        this.operations = operations;
    }

    /** Allow-listed MDMS module and master name exposed through {@code lookup_master}. */
    public static class MasterRef {
        /** MDMS module name (for example {@code RAINMAKER-PGR}). */
        private String module;
        /** Master detail name (for example {@code ServiceDefs}). */
        private String name;

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * One read or write operation under a service. Gateway path and templates are server-side only;
     * the assistant sees {@link #getInputSchema()} via {@code describe_operation}.
     */
    public static class OperationDescriptor {
        /** Operation key ({@code search}, {@code create}, …). */
        private String name;
        private String description;
        private String type;
        private boolean readOnly;
        private boolean destructive;
        private boolean requiresConfirmation;
        private String httpMethod;
        private String gatewayPath;
        private String filestoreModule;
        private String documentsTarget;
        private JsonNode inputSchema;
        private JsonNode requestBody;
        private JsonNode query;
        private JsonNode response;
        private List<Map<String, Object>> examples = new ArrayList<>();
        private boolean supportsStatus;
        private String statusArgument;
        private boolean statusArgumentAsList;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public boolean isDestructive() {
            return destructive;
        }

        public void setDestructive(boolean destructive) {
            this.destructive = destructive;
        }

        public boolean isRequiresConfirmation() {
            return requiresConfirmation;
        }

        public void setRequiresConfirmation(boolean requiresConfirmation) {
            this.requiresConfirmation = requiresConfirmation;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getGatewayPath() {
            return gatewayPath;
        }

        public void setGatewayPath(String gatewayPath) {
            this.gatewayPath = gatewayPath;
        }

        public String getFilestoreModule() {
            return filestoreModule;
        }

        public void setFilestoreModule(String filestoreModule) {
            this.filestoreModule = filestoreModule;
        }

        public String getDocumentsTarget() {
            return documentsTarget;
        }

        public void setDocumentsTarget(String documentsTarget) {
            this.documentsTarget = documentsTarget;
        }

        public JsonNode getInputSchema() {
            return inputSchema;
        }

        public void setInputSchema(JsonNode inputSchema) {
            this.inputSchema = inputSchema;
        }

        public JsonNode getRequestBody() {
            return requestBody;
        }

        public void setRequestBody(JsonNode requestBody) {
            this.requestBody = requestBody;
        }

        public JsonNode getQuery() {
            return query;
        }

        public void setQuery(JsonNode query) {
            this.query = query;
        }

        public JsonNode getResponse() {
            return response;
        }

        public void setResponse(JsonNode response) {
            this.response = response;
        }

        public List<Map<String, Object>> getExamples() {
            return examples;
        }

        public void setExamples(List<Map<String, Object>> examples) {
            this.examples = examples;
        }

        public boolean isSupportsStatus() {
            return supportsStatus;
        }

        public void setSupportsStatus(boolean supportsStatus) {
            this.supportsStatus = supportsStatus;
        }

        public String getStatusArgument() {
            return statusArgument;
        }

        public void setStatusArgument(String statusArgument) {
            this.statusArgument = statusArgument;
        }

        public boolean isStatusArgumentAsList() {
            return statusArgumentAsList;
        }

        public void setStatusArgumentAsList(boolean statusArgumentAsList) {
            this.statusArgumentAsList = statusArgumentAsList;
        }
    }
}
