package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.asset.UserIncome;

import java.util.Collections;
import java.util.List;

/**
 * Standard UPYOG response contract for citizen income operations.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Response envelope for citizen income mutations and search queries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IncomeResponse {

    /**
     * Standard UPYOG ResponseInfo contract header.
     */
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    /**
     * List of retrieved or mutated citizen income declarations.
     */
    @JsonProperty("incomes")
    @Builder.Default
    private List<UserIncome> incomes = Collections.emptyList();

    /**
     * Total count of income records returned.
     */
    @JsonProperty("totalCount")
    private Integer totalCount;

    /**
     * Convenience constructor wrapping a single income entity.
     *
     * @param responseInfo response status metadata
     * @param income       the created, updated, or deactivated income entity
     */
    public IncomeResponse(ResponseInfo responseInfo, UserIncome income) {
        this.responseInfo = responseInfo;
        this.incomes = income != null ? Collections.singletonList(income) : Collections.emptyList();
        this.totalCount = this.incomes.size();
    }
}
