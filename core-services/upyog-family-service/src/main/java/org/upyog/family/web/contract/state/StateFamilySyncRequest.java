package org.upyog.family.web.contract.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request payload from State Wrapper pushing State Family & Members into UPYOG (UC2).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateFamilySyncRequest {

    @NotNull(message = "RequestInfo cannot be null")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Valid
    @NotNull(message = "StateFamily cannot be null")
    @JsonProperty("StateFamily")
    private StateFamily stateFamily;
}
