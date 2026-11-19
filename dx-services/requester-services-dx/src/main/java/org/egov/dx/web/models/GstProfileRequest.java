package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
     * The 15-character alphanumeric Goods and Services Tax Identification Number (e.g. 03AAAPS0001B1Z5).
     */
    @JsonProperty("gstin")
    private String gstin;

    /**
     * Mandatory tenant identifier for municipal context and MDMS verification governance check (e.g. "pg.citya").
     */
    @JsonProperty("tenantId")
    private String tenantId;
}
