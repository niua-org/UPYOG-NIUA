package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;

import java.io.Serializable;

/**
 * Response payload returning the Head of Family (HoF) evaluation result for a verified Aadhaar citizen.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Response envelope containing Head of Family (HoF) verification result")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarHofCheckResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Standard UPYOG ResponseInfo contract")
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @Schema(description = "Indicates whether the citizen is the active Head of Family (HoF)", example = "true")
    @JsonProperty("isHof")
    private Boolean isHof;

    @Schema(description = "Unique user UUID of the matched citizen", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    @JsonProperty("userUuid")
    private String userUuid;

    @Schema(description = "Full name of the verified citizen", example = "Ramesh Kumar")
    @JsonProperty("userName")
    private String userName;

    @Schema(description = "Masked Aadhaar number of the citizen", example = "XXXXXXXX1234")
    @JsonProperty("maskedAadhaar")
    private String maskedAadhaar;

    @Schema(description = "Unique family identifier to which the citizen is mapped as Head", example = "FAM-2026-0001")
    @JsonProperty("familyId")
    private String familyId;

    @Schema(description = "Tenant identifier of the family and citizen", example = "pg.citya")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Informative status message", example = "Citizen is verified as Head of Family")
    @JsonProperty("message")
    private String message;
}
