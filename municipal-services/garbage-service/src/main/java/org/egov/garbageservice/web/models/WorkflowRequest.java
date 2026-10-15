package org.egov.garbageservice.web.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.tracer.annotations.CustomSafeHtml;

import java.util.List;

@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Holds workflow action and documents from the new payload structure.
 * action and documents are mapped to GarbageAccount fields during enrichment.
 */
public class WorkflowRequest {

    @JsonProperty("action")
    @CustomSafeHtml
    private String action;

    @JsonProperty("comments")
    @CustomSafeHtml
    private String comments;

    @JsonProperty("assignes")
    private List<String> assignes;

    @JsonProperty("documents")
    private List<GrbgDocument> documents;
}
