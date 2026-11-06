package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;

import java.util.List;

/**
 * Canonical response wrapper for Garbage Service.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("garbageApplications")
    private List<GarbageApplication> garbageApplications;

    @JsonProperty("totalCount")
    private Integer totalCount;

    public static GarbageResponse of(ResponseInfo responseInfo, List<GarbageApplication> applications) {
        return GarbageResponse.builder()
                .responseInfo(responseInfo)
                .garbageApplications(applications)
                .totalCount(applications != null ? applications.size() : 0)
                .build();
    }
}
