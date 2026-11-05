package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
 * Canonical multi-application request wrapper for Garbage Service.
 * Handles both owner and tenant applications in a single payload.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageRequest {

    @NotNull
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Valid
    @JsonProperty("garbageApplications")
    private List<GarbageApplication> garbageApplications;

    // Backward compatibility with legacy frontend payloads
    @Valid
    @JsonProperty("garbageAccounts")
    private List<GarbageApplication> garbageAccounts;

    @Builder.Default
    @JsonProperty("fromMigration")
    private Boolean fromMigration = false;

    @Builder.Default
    @JsonProperty("createChildAccountOnly")
    private Boolean createChildAccountOnly = false;

    public List<GarbageApplication> getGarbageApplications() {
        if (garbageApplications != null && !garbageApplications.isEmpty()) {
            return garbageApplications;
        }
        if (garbageAccounts != null && !garbageAccounts.isEmpty()) {
            return garbageAccounts;
        }
        if (garbageApplications == null) {
            garbageApplications = new ArrayList<>();
        }
        return garbageApplications;
    }
    public GarbageRequest(RequestInfo requestInfo, List<GarbageApplication> garbageApplications) {
        this.requestInfo = requestInfo;
        this.garbageApplications = garbageApplications;
    }
}
