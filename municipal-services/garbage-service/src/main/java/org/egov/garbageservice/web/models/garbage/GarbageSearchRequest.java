package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request wrapper for garbage application search.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageSearchRequest {

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Valid
    @JsonProperty("searchCriteria")
    @JsonAlias({"searchCriteriaGarbageAccount"})
    private GarbageSearchCriteria searchCriteria;

    public GarbageSearchCriteria getSearchCriteriaGarbageAccount() {
        return searchCriteria;
    }

    public void setSearchCriteriaGarbageAccount(GarbageSearchCriteria searchCriteria) {
        this.searchCriteria = searchCriteria;
    }
}
