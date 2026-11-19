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
import org.egov.user.domain.model.asset.UserProperty;

import java.util.Collections;
import java.util.List;

/**
 * Standard UPYOG response contract for rural property operations.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Response envelope for rural property mutations and search queries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RuralPropertyResponse {

    /**
     * Standard UPYOG ResponseInfo contract header.
     */
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    /**
     * List of retrieved or mutated rural properties.
     */
    @JsonProperty("properties")
    @Builder.Default
    private List<UserProperty> properties = Collections.emptyList();

    /**
     * Total count of properties returned.
     */
    @JsonProperty("totalCount")
    private Integer totalCount;

    /**
     * Convenience constructor wrapping a single property entity.
     *
     * @param responseInfo response status metadata
     * @param property     the created, updated, or deactivated property
     */
    public RuralPropertyResponse(ResponseInfo responseInfo, UserProperty property) {
        this.responseInfo = responseInfo;
        this.properties = property != null ? Collections.singletonList(property) : Collections.emptyList();
        this.totalCount = this.properties.size();
    }
}
