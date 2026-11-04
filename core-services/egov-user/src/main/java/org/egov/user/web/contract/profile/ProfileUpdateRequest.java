package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.web.contract.UserRequest;

/**
 * Request DTO for updating citizen profile details, supporting demographic enhancements and Aadhaar re-sync.
 */
@Schema(description = "Request payload for updating citizen profile details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "Citizen profile update payload (canonical attribute name)")
    @Valid
    @JsonProperty("profile")
    private UserRequest profile;

    @Schema(description = "Citizen profile update payload (legacy/fallback attribute name)")
    @Valid
    @JsonProperty("user")
    private UserRequest user;

    @Schema(description = "Flag indicating whether this update is a re-synchronization from Aadhaar e-KYC", example = "false")
    @JsonProperty("isResync")
    @Builder.Default
    private Boolean isResync = false;

    @Schema(description = "Aadhaar number if re-synchronizing with OTP", example = "XXXXXXXX1234")
    @JsonProperty("aadhaarNumber")
    private String aadhaarNumber;

    @Schema(description = "UIDAI transaction ID for OTP verification", example = "txn-5678-abcd")
    @JsonProperty("txnId")
    private String txnId;

    @Schema(description = "UIDAI OTP for Aadhaar re-verification", example = "123456")
    @JsonProperty("otp")
    private String otp;

    /**
     * Resolves the effective profile {@link UserRequest} payload from either {@code profile} or {@code user}.
     *
     * @return the resolved {@link UserRequest}
     */
    @JsonIgnore
    public UserRequest getEffectiveProfile() {
        return profile != null ? profile : user;
    }
}
