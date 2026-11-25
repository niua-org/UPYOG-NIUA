package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.tracer.annotations.CustomSafeHtml;

@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
/**
 * Applicant person details from the payload structure (applicantDetails array).
 * Supports both String values and object wrappers for gender and relationShipType.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicantDetail {

    @CustomSafeHtml
    private String applicantName;

    @CustomSafeHtml
    private String name;

    @CustomSafeHtml
    private String mobileNumber;

    @CustomSafeHtml
    private String alternateNumber;

    @CustomSafeHtml
    private String gender;

    @CustomSafeHtml
    private String dateOfBirth;

    @CustomSafeHtml
    private String relationShipType;

    @CustomSafeHtml
    private String guardianName;

    @CustomSafeHtml
    private String emailId;

    @JsonProperty("gender")
    public void setGender(JsonNode node) {
        if (node == null || node.isNull()) {
            this.gender = null;
        } else if (node.isTextual()) {
            this.gender = node.asText();
        } else if (node.has("code")) {
            this.gender = node.get("code").asText();
        } else if (node.has("name")) {
            this.gender = node.get("name").asText();
        } else {
            this.gender = node.asText();
        }
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @JsonProperty("relationShipType")
    public void setRelationShipType(JsonNode node) {
        if (node == null || node.isNull()) {
            this.relationShipType = null;
        } else if (node.isTextual()) {
            this.relationShipType = node.asText();
        } else if (node.has("code")) {
            this.relationShipType = node.get("code").asText();
        } else if (node.has("name")) {
            this.relationShipType = node.get("name").asText();
        } else {
            this.relationShipType = node.asText();
        }
    }

    public void setRelationShipType(String relationShipType) {
        this.relationShipType = relationShipType;
    }

    @JsonProperty("applicantName")
    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
        if (this.name == null) {
            this.name = applicantName;
        }
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
        if (this.applicantName == null) {
            this.applicantName = name;
        }
    }
}
