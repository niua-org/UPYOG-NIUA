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
import org.egov.dx.util.TanConstants;

/**
 * Controller request DTO for TAN verification in requester-services-dx.
 *
 * <p>Strictly excludes raw {@code tan} from {@link #toString()} to prevent PII exposure in logs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"tan"})
public class TanVerificationRequest {

    /**
     * Standard eGov RequestInfo header containing audit, auth token, and API metadata.
     */
    @NotNull(message = TanConstants.MSG_INVALID_REQUEST)
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * The 10-character alphanumeric Tax Deduction and Collection Account Number (e.g. BLRM12345C).
     */
    @JsonProperty("tan")
    private String tan;

    /**
     * Mandatory tenant identifier for municipal context and MDMS verification governance check (e.g. "pg.citya").
     */
    @JsonProperty("tenantId")
    private String tenantId;
}
