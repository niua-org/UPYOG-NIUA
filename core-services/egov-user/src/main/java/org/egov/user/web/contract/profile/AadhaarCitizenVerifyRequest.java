package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Request envelope for verifying Aadhaar OTP and resolving or auto-provisioning citizen profile.
 *
 * Used during family member addition flow by the Head of Family (HOF).
 */
@Schema(description = "Request envelope for Aadhaar OTP verification and citizen identity resolution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarCitizenVerifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Standard UPYOG RequestInfo contract", required = true)
    @JsonProperty("RequestInfo")
    @NotNull(message = "RequestInfo cannot be null")
    private RequestInfo requestInfo;

    @Schema(description = "Aadhaar e-KYC transaction or reference ID from OTP generation", example = "298662")
    @JsonProperty("referenceId")
    private String referenceId;

    @Schema(description = "One-time password (OTP) received on citizen's Aadhaar-linked mobile", example = "123456", required = true)
    @JsonProperty("otp")
    private String otp;

    @Schema(description = "Aadhaar number of the citizen being added (12-digit plain or masked)", example = "123456789012")
    @JsonProperty("aadhaarNumber")
    private String aadhaarNumber;

    @Schema(description = "Mobile number of the citizen if known", example = "9876543210")
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @Schema(description = "Tenant identifier", example = "pg.citya")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Citizen name if available prior to verification", example = "Jane Doe")
    @JsonProperty("name")
    private String name;

    /**
     * Resolves effective referenceId.
     *
     * @return non-empty reference ID string, or null if not provided
     */
    public String getEffectiveReferenceId() {
        return StringUtils.isNotBlank(referenceId) ? referenceId.trim() : null;
    }
}
