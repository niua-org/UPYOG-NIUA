package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.response.ResponseInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Response payload returned after synchronizing citizen profile demographics and asset declarations.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MemberAssetSyncResponse {

    /**
     * Standard UPYOG ResponseInfo contract.
     */
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    /**
     * Overall synchronization status.
     */
    @JsonProperty("status")
    private String status;

    /**
     * Detailed synchronization result for each processed member.
     */
    @JsonProperty("results")
    @Builder.Default
    private List<MemberAssetSyncResult> results = new ArrayList<>();
}
