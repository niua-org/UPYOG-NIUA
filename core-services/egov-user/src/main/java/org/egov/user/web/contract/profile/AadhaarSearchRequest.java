package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dedicated request contract for searching a citizen profile by UIDAI Aadhaar number.
 * Access to this search operation is strictly restricted to trusted internal microservices.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Request payload for dedicated citizen lookup by Aadhaar number")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarSearchRequest {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    @NotNull(message = "RequestInfo is mandatory")
    private RequestInfo requestInfo;

    @Schema(description = "Tenant identifier", example = "pg")
    private String tenantId;

    @Schema(description = "12-digit UIDAI Aadhaar number", example = "548796521458")
    @NotBlank(message = "Aadhaar number is mandatory")
    private String aadhaarNumber;
}
