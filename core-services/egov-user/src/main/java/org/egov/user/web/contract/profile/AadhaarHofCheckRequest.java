package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

import java.io.Serializable;

/**
 * Request payload for checking if a citizen with the given Aadhaar number exists
 * and is the designated Head of Family (HoF) of an active family.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Request envelope for verifying citizen existence and Head of Family (HoF) status by Aadhaar number")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarHofCheckRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Standard UPYOG RequestInfo contract", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("RequestInfo")
    @NotNull(message = "RequestInfo cannot be null")
    private RequestInfo requestInfo;

    @Schema(description = "12-digit UIDAI Aadhaar number of the citizen", example = "123456789012", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("aadhaarNumber")
    @NotBlank(message = "Aadhaar number is mandatory")
    private String aadhaarNumber;

    @Schema(description = "Tenant identifier scoping the citizen and family search", example = "pg.citya")
    @JsonProperty("tenantId")
    private String tenantId;
}
