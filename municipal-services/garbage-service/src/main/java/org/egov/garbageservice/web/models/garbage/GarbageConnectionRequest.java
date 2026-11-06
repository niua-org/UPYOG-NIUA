package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Request wrapper for connection lifecycle operations (disconnect, reconnect, permanent disconnect).
 */
@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class GarbageConnectionRequest {

    @NotNull
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotNull
    @Valid
    @JsonProperty("connectionLog")
    private GarbageConnectionLog connectionLog;

    @JsonProperty("targetApplicationNos")
    @Builder.Default
    private List<String> targetApplicationNos = new ArrayList<>();
}

