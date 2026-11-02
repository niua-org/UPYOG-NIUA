package org.upyog.family.web.contract.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.response.ResponseInfo;

/**
 * Response payload returned after syncing or fetching State Family data in UPYOG (UC2).
 * Contains sanitized/masked member demographics for display on the "Family Found" UI screen.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateFamilySyncResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("referenceId")
    private String referenceId;

    @JsonProperty("stateFamily")
    private StateFamily stateFamily;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;
}
