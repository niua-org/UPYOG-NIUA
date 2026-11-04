package org.upyog.family.web.contract.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request payload for Just-In-Time (JIT) provisioning and member mapping in UPYOG (UC2).
 * Invoked once the member OTP is verified to ensure Family exists and Member is mapped.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateFamilyJitProvisionRequest {

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @JsonProperty("referenceId")
    private String referenceId;

    @NotBlank(message = "stateFamilyId is mandatory")
    @JsonProperty("stateFamilyId")
    private String stateFamilyId;

    @NotBlank(message = "tenantId is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(message = "userUuid of authenticated citizen is mandatory")
    @JsonProperty("userUuid")
    private String userUuid;

    @JsonProperty("selectedMemberId")
    private String selectedMemberId;

    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @JsonProperty("aadhaarNumber")
    private String aadhaarNumber;
}
