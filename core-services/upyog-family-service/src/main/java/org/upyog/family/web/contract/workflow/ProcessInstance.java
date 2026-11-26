package org.upyog.family.web.contract.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.request.User;
import org.upyog.family.web.contract.AuditDetails;

import java.util.List;

/**
 * Workflow ProcessInstance contract representing the state and progress of an entity through workflow.
 */
@Schema(description = "Workflow ProcessInstance contract representing the state and progress of an entity through workflow.")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class ProcessInstance {

    @Size(max = 64)
    @JsonProperty("id")
    private String id;

    @NotNull
    @Size(max = 128)
    @JsonProperty("tenantId")
    private String tenantId;

    @NotNull
    @Size(max = 128)
    @JsonProperty("businessService")
    private String businessService;

    @NotNull
    @Size(max = 128)
    @JsonProperty("businessId")
    private String businessId;

    @NotNull
    @Size(max = 128)
    @JsonProperty("action")
    private String action;

    @NotNull
    @Size(max = 64)
    @JsonProperty("moduleName")
    private String moduleName;

    @JsonProperty("state")
    private State state;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("documents")
    @Valid
    private List<Document> documents;

    @JsonProperty("assigner")
    private User assigner;

    @JsonProperty("assignee")
    private User assignee;

    @JsonProperty("nextActions")
    @Valid
    private List<Action> nextActions;

    @JsonProperty("stateSla")
    private Long stateSla;

    @JsonProperty("businesssServiceSla")
    private Long businesssServiceSla;

    @JsonProperty("previousStatus")
    @Size(max = 128)
    private String previousStatus;

    @JsonProperty("entity")
    private Object entity;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;

    @JsonProperty("assignes")
    private List<User> assignes;
}
