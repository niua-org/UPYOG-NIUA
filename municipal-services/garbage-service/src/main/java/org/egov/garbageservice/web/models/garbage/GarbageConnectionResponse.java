package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for connection lifecycle operations.
 */
@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class GarbageConnectionResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("connectionLog")
    private GarbageConnectionLog connectionLog;

    @JsonProperty("affectedApplications")
    @Builder.Default
    private List<GarbageApplication> affectedApplications = new ArrayList<>();
}

