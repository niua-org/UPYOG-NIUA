package org.egov.user.web.contract.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;

/**
 * Response payload confirming property consent token validity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanPropertyConsentValidateResponse {

    /**
     * Standard platform response header indicating API execution status.
     */
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    /**
     * Validation outcome indicating whether the consent token or verification session is valid and unexpired.
     */
    @JsonProperty("valid")
    private Boolean valid;

    /**
     * Unique property identifier evaluated during validation.
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Descriptive validation message.
     */
    @JsonProperty("message")
    private String message;
}
