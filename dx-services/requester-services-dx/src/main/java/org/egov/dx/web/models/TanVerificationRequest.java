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
    @NotBlank(message = TanConstants.MSG_BLANK_TAN)
    @Pattern(regexp = TanConstants.TAN_REGEX, message = TanConstants.MSG_INVALID_FORMAT)
    @JsonProperty("tan")
    private String tan;

    /**
     * Optional deductor name to match against the TAN registry.
     */
    @Size(max = 200, message = "Deductor name must not exceed 200 characters")
    @JsonProperty("deductorName")
    private String deductorName;

    /**
     * Mandatory tenant identifier for municipal context and MDMS verification governance check (e.g. "pg.citya").
     */
    @NotBlank(message = "Tenant ID is mandatory for TAN verification.")
    @Size(min = 2, max = 64, message = "Tenant ID must be between 2 and 64 characters")
    @JsonProperty("tenantId")
    private String tenantId;
}
