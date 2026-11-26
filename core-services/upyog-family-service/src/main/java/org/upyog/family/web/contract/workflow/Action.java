package org.upyog.family.web.contract.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;
import org.upyog.family.web.contract.AuditDetails;

import java.util.List;

/**
 * Workflow action model representing permitted state transitions.
 */
@Schema(description = "Workflow action model representing permitted state transitions.")
@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class Action {

    @Size(max = 256)
    @JsonProperty("uuid")
    private String uuid;

    @Size(max = 256)
    @JsonProperty("tenantId")
    private String tenantId;

    @Size(max = 256)
    @JsonProperty("currentState")
    private String currentState;

    @Size(max = 256)
    @JsonProperty("action")
    private String action;

    @Size(max = 256)
    @JsonProperty("nextState")
    private String nextState;

    @Size(max = 1024)
    @JsonProperty("roles")
    @Valid
    private List<String> roles;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
