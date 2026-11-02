package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.web.contract.UserSearchResponseContent;

import java.util.List;

/**
 * Standard response envelope for citizen profile operations.
 */
@Schema(description = "Response envelope containing citizen profile details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    @Schema(description = "Standard UPYOG ResponseInfo contract")
    @JsonProperty("responseInfo")
    private ResponseInfo responseInfo;

    @Schema(description = "List of returned user profiles")
    @JsonProperty("user")
    private List<UserSearchResponseContent> user;
}
