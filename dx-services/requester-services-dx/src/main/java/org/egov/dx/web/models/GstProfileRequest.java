package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.util.GstConstants;

/**
 * Controller request DTO for GST profile search in requester-services-dx.
 *
 * <p>Strictly excludes raw {@code gstin} from {@link #toString()} to prevent PII exposure in logs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"gstin"})
public class GstProfileRequest {

    /**
     * Standard eGov RequestInfo header containing audit, auth token, and API metadata.
     */
    @NotNull(message = GstConstants.MSG_INVALID_REQUEST)
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * The 15-character alphanumeric Goods and Services Tax Identification Number (e.g. 03AAAPS0001B1ZW).
     */
    @NotBlank(message = GstConstants.MSG_BLANK_GSTIN)
    @Pattern(regexp = GstConstants.GSTIN_REGEX, message = GstConstants.MSG_INVALID_FORMAT)
    @JsonProperty("gstin")
    private String gstin;

    /**
     * Mandatory tenant identifier for municipal context and MDMS verification governance check (e.g. "pg.citya").
     */
    @NotBlank(message = "Tenant ID is mandatory for GST verification.")
    @Size(min = 2, max = 64, message = "Tenant ID must be between 2 and 64 characters")
    @JsonProperty("tenantId")
    private String tenantId;
}
