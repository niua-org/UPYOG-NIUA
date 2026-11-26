package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.response.ResponseInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard UPYOG API response envelope returning family aggregates.
 *
 * <p>Wraps a list of resolved {@link Family} aggregates alongside platform {@link ResponseInfo} headers
 * and pagination totals.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyResponse {

    /**
     * Platform response status and correlation metadata.
     */
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    /**
     * List of family aggregates returned by the query or mutation.
     */
    @JsonProperty("Families")
    @Builder.Default
    private List<Family> families = new ArrayList<>();

    /**
     * Total number of family records matching search criteria (useful for pagination).
     */
    @JsonProperty("totalCount")
    private Integer totalCount;
}
