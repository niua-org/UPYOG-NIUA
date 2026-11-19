package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request contract for validating OTP sent to Head of Family for joining verification in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyOtpVerifyRequest {

    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(message = "familyId is mandatory")
    @JsonProperty("familyId")
    private String familyId;

    @NotBlank(message = "tenantId is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(message = "otp is mandatory")
    @Pattern(regexp = "^\\d{4,6}$", message = "otp must be a numeric string between 4 and 6 digits")
    @JsonProperty("otp")
    private String otp;

    @NotBlank(message = "referenceId is mandatory")
    @JsonProperty("referenceId")
    private String referenceId;
}
