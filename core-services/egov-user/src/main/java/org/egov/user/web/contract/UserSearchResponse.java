package org.egov.user.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.User;

import java.util.List;

/**
 * Web contract response payload for user search endpoints.
 * <p>
 * Contains {@link ResponseInfo} status headers along with the list of matching
 * {@link UserSearchResponseContent} user records.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserSearchResponse {
    @JsonProperty("responseInfo")
    ResponseInfo responseInfo;

    @JsonProperty("user")
    List<UserSearchResponseContent> userSearchResponseContent;

}
