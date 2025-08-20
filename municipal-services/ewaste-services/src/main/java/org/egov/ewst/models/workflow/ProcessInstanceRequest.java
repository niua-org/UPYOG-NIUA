package org.egov.ewst.models.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;

/**
 * Contract class to receive process instance request.
 */
@ApiModel(description = "Contract class to process instance receive request. Array of Ewaste items are used in case of create, whereas single Ewaste item is used for update")
@Validated
@jakarta.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2018-12-04T11:26:25.532+05:30")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ProcessInstanceRequest {
        @JsonProperty("RequestInfo")
        private RequestInfo requestInfo;

        @JsonProperty("ProcessInstances")
        @Valid
        @NotNull
        private List<ProcessInstance> processInstances;

    /**
     * Adds a process instance to the list of process instances in the request.
     *
     * @param processInstanceItem the process instance to add
     * @return the updated ProcessInstanceRequest object
     */
        public ProcessInstanceRequest addProcessInstanceItem(ProcessInstance processInstanceItem) {
            if (this.processInstances == null) {
            this.processInstances = new ArrayList<>();
            }
        this.processInstances.add(processInstanceItem);
        return this;
        }

}

