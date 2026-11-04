package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.web.contract.UserSearchResponseContent;

import java.io.Serializable;

/**
 * Response envelope containing Aadhaar verified citizen details and identity resolution outcome.
 */
@Schema(description = "Response envelope containing Aadhaar verified citizen details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarCitizenVerifyResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Standard UPYOG ResponseInfo contract")
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @Schema(description = "Aadhaar verified or provisioned citizen profile details")
    @JsonProperty("user")
    private UserSearchResponseContent user;

    @Schema(description = "Flag indicating whether this citizen was newly created (true) or already existed in the system (false)")
    @JsonProperty("isNewUser")
    private Boolean isNewUser;
}
