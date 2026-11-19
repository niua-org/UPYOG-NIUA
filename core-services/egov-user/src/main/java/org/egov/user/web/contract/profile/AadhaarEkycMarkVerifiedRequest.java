package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

import java.io.Serializable;

/**
 * Inter-service request to mark a citizen as Aadhaar identity-verified after successful e-KYC OTP verification.
 *
 * <p>TODO: mask Aadhaar and move plain-text storage to Aadhaar Data Vault when integrating with UIDAI.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarEkycMarkVerifiedRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank
    @JsonProperty("userUuid")
    private String userUuid;

    @NotBlank
    @JsonProperty("aadhaarNumber")
    private String aadhaarNumber;

    @JsonProperty("tenantId")
    private String tenantId;
}
