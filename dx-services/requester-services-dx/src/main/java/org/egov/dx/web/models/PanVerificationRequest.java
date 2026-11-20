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
import org.egov.dx.util.PanConstants;

/**
 * Controller request DTO for PAN verification in requester-services-dx.
 *
 * <p>Strictly excludes raw {@code pan}, {@code fullName}, and {@code dob} from {@link #toString()} to prevent PII exposure in logs.</p>
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
    @NotBlank(message = PanConstants.MSG_BLANK_PAN)
    @Pattern(regexp = PanConstants.PAN_REGEX, message = PanConstants.MSG_INVALID_FORMAT)
    @JsonProperty("pan")
    private String pan;

    /**
     * Full name as on the PAN card or applicant legal name.
     */
    @NotBlank(message = PanConstants.MSG_MISSING_NAME)
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    @JsonProperty("fullName")
    private String fullName;

    /**
     * Date of birth or incorporation (YYYY-MM-DD or DD/MM/YYYY).
     */
    @NotBlank(message = PanConstants.MSG_MISSING_DOB)
    @Pattern(regexp = "^(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{2}-\\d{2}-\\d{4})$",
            message = "Date of birth must be in YYYY-MM-DD or DD/MM/YYYY format")
    @JsonProperty("dob")
    private String dob;

    /**
     * Optional gender validated dynamically against MDMS common-masters.GenderType.
     */
    @JsonProperty("gender")
    private String gender;

    /**
     * Mandatory tenant identifier for municipal context and MDMS verification governance check (e.g. "pg.citya").
     */
    @NotBlank(message = "Tenant ID is mandatory for PAN verification.")
    @Size(min = 2, max = 64, message = "Tenant ID must be between 2 and 64 characters")
    @JsonProperty("tenantId")
    private String tenantId;
}
