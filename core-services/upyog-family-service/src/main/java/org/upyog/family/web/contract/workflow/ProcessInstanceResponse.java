package org.upyog.family.web.contract.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.response.ResponseInfo;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Contract class to receive process instance transition responses from egov-workflow-v2.
 */
@Schema(description = "Contract class to receive process instance transition responses from egov-workflow-v2.")
@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ProcessInstanceResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("ProcessInstances")
    @Valid
    private List<ProcessInstance> processInstances;
}
