package org.egov.user.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.egov.common.contract.response.ResponseInfo;

import java.util.List;

/**
 * Web contract response payload returning user profile details in V2 format.
 * <p>
 * Contains {@link ResponseInfo} status headers along with the list of matching {@link UserRequestV2} records
 * with extended demographic and multi-address attributes.
 */
@AllArgsConstructor
@Getter
public class UserDetailResponseV2 {
    @JsonProperty("responseInfo")
    ResponseInfo responseInfo;

    @JsonProperty("user")
    List<UserRequestV2> user;
}
