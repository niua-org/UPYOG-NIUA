package org.egov.dx.upyogdata.pfms.models;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

import java.util.List;

/**
 * Request payload used by state systems to submit PFMS transaction data.
 *
 * Contains RequestInfo for request context/authentication information
 * and one or more PFMS data records
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PFMSCreateTransactionRequest {

    @JsonProperty("RequestInfo")
    @NotNull(message = "RequestInfo is mandatory")
    @Valid
    private RequestInfo requestInfo;

    @NotEmpty(message = "Data cannot be empty")
    @Valid
    private List<PFMSData> data;
}
