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
import org.egov.dx.util.PanConstants;

/**
 * Controller request DTO for PAN verification in requester-services-dx.
 *
 * <p>Strictly excludes raw {@code pan} from {@link #toString()} to prevent PII exposure in logs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"pan", "fullName", "dob"})
public class PanVerificationRequest {

    /**
     * Standard eGov RequestInfo header containing audit, auth token, and API metadata.
     */
    @NotNull(message = PanConstants.MSG_INVALID_REQUEST)
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * The 10-character alphanumeric Permanent Account Number (e.g. ABCDE1234F).
     */
    @JsonProperty("pan")
    private String pan;

    /**
     * Full name as on the PAN card or applicant legal name.
     */
    @JsonProperty("fullName")
    private String fullName;

    /**
     * Date of birth or incorporation (YYYY-MM-DD or DD/MM/YYYY).
     */
    @JsonProperty("dob")
    private String dob;

    /**
     * Optional gender ("M", "F", "O").
     */
    @JsonProperty("gender")
    private String gender;

    /**
     * Mandatory tenant identifier for municipal context and MDMS verification governance check (e.g. "pg.citya").
     */
    @JsonProperty("tenantId")
    private String tenantId;
}
