package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.request.RequestInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Request payload for synchronizing citizen profile demographics and asset declarations
 * from caller services (e.g. {@code upyog-family-service}) to {@code egov-user}.
 *
 * <p>Supports both batch synchronization via {@code members} and single-member synchronization
 * via {@code member}.</p>
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
public class MemberAssetSyncRequest {

    /**
     * Standard UPYOG RequestInfo contract.
     */
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Batch of family member asset declarations to synchronize.
     */
    @Valid
    @JsonProperty("members")
    @Builder.Default
    private List<MemberAssetDetail> members = new ArrayList<>();

    /**
     * Single member asset declaration convenience payload.
     */
    @Valid
    @JsonProperty("member")
    private MemberAssetDetail member;

    /**
     * Normalizes the list of members to process, combining {@code members} and {@code member}.
     *
     * @return non-null list of {@link MemberAssetDetail}
     */
    public List<MemberAssetDetail> getNormalizedMembers() {
        if (members != null && !members.isEmpty()) {
            return members;
        }
        if (member != null) {
            return Collections.singletonList(member);
        }
        return Collections.emptyList();
    }
}
