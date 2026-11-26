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
 * Workflow State model representing lifecycle phases in business services.
 */
@Schema(description = "Workflow State model representing lifecycle phases in business services.")
@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class State {

    @Size(max = 256)
    @JsonProperty("uuid")
    private String uuid;

    @Size(max = 256)
    @JsonProperty("tenantId")
    private String tenantId;

    @Size(max = 256)
    @JsonProperty("businessServiceId")
    private String businessServiceId;

    @JsonProperty("sla")
    private Long sla;

    @Size(max = 256)
    @JsonProperty("state")
    private String state;

    @Size(max = 256)
    @JsonProperty("applicationStatus")
    private String applicationStatus;

    @JsonProperty("docUploadRequired")
    private Boolean docUploadRequired;

    @JsonProperty("isStartState")
    private Boolean isStartState;

    @JsonProperty("isTerminateState")
    private Boolean isTerminateState;

    @JsonProperty("isStateUpdatable")
    private Boolean isStateUpdatable;

    @JsonProperty("actions")
    @Valid
    private List<Action> actions;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
