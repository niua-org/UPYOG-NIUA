package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.response.ResponseInfo;

/**
 * Response contract returned upon successful OTP dispatch to the Head of Family in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyOtpResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("familyId")
    private String familyId;

    @JsonProperty("maskedMobileNumber")
    private String maskedMobileNumber;

    @JsonProperty("referenceId")
    private String referenceId;

    @JsonProperty("isSent")
    private Boolean isSent;
}
