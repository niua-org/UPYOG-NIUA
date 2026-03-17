package org.upyog.employee.dasboard.web.models;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.egov.common.contract.request.RequestInfo;
import lombok.Data;

@Data
public class RoleBasedDashboardRequest {
    @Valid
    @NotNull
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;
}
