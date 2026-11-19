package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;
import org.upyog.family.web.contract.workflow.Document;

import java.util.List;

/**
 * Represents a workflow object used for tracking actions, status, and related details
 * in the Family lifecycle processes (create, verify, approve, reject).
 */
@Schema(description = "Represents a workflow object used for tracking actions, status, and related details in the Family service.")
@Validated
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Workflow {

    @JsonProperty("action")
    private String action;

    @JsonProperty("status")
    private String status;

    @JsonProperty("comments")
    @JsonAlias({"comment", "comments"})
    private String comments;

    @JsonProperty("assignes")
    @JsonAlias({"assignees", "assignes"})
    @Valid
    private List<String> assignes;

    @JsonProperty("documents")
    @Valid
    private List<Document> documents;

    @JsonProperty("businessService")
    private String businessService;

    @JsonProperty("moduleName")
    private String moduleName;

    @JsonProperty("rating")
    private Integer rating;
}
