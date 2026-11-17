package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;

import java.util.List;

/**
 * Search criteria contract for querying citizen rural properties from {@code ug_user_property}
 * and {@code ug_user_rural_property}.
 *
 * <p>Supports both flat and nested payloads matching standard UPYOG criteria conventions.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Search criteria envelope for querying citizen rural property assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RuralPropertySearchCriteria {

    /**
     * Standard UPYOG RequestInfo contract.
     */
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Nested criteria wrapper for clients wrapping filters inside {@code criteria}.
     */
    @JsonProperty("criteria")
    private RuralPropertySearchCriteria criteria;

    /**
     * Citizen {@code uuid} owning the property.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * List of citizen UUIDs to filter properties across multiple household members.
     */
    @JsonProperty("userUuids")
    private List<String> userUuids;

    /**
     * Unique property primary key ID.
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * List of property primary key IDs.
     */
    @JsonProperty("propertyIds")
    private List<String> propertyIds;

    /**
     * Tenant identifier scoping the property.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Gram Panchayat administrative jurisdiction.
     */
    @JsonProperty("gramPanchayat")
    private String gramPanchayat;

    /**
     * Revenue village name.
     */
    @JsonProperty("villageName")
    private String villageName;

    /**
     * Land revenue survey or Khasra plot identifier.
     */
    @JsonProperty("surveyNumber")
    private String surveyNumber;

    /**
     * Lifecycle status filter (e.g. {@code ACTIVE}, {@code INACTIVE}, or {@code ALL}).
     * Defaults to {@code ACTIVE} when null or blank.
     */
    @JsonProperty("status")
    private String status;

    /**
     * Flag indicating whether inactive/deactivated properties should be included.
     */
    @JsonProperty("includeDeactivated")
    private Boolean includeDeactivated;

    /**
     * Pagination offset.
     */
    @JsonProperty("offset")
    private Integer offset;

    /**
     * Pagination limit.
     */
    @JsonProperty("limit")
    private Integer limit;

    /**
     * Unwraps nested criteria if passed inside {@code criteria} property.
     *
     * @return normalized effective search criteria
     */
    public RuralPropertySearchCriteria resolveCriteria() {
        if (this.criteria != null) {
            if (this.criteria.getRequestInfo() == null) {
                this.criteria.setRequestInfo(this.requestInfo);
            }
            if (this.criteria.getLimit() == null) {
                this.criteria.setLimit(this.limit);
            }
            if (this.criteria.getOffset() == null) {
                this.criteria.setOffset(this.offset);
            }
            return this.criteria;
        }
        return this;
    }

    /**
     * Determines whether any query filter was specified to prevent uncontrolled full table scans.
     *
     * @return {@code true} if at least one meaningful filter is present; {@code false} otherwise
     */
    public boolean hasAnyFilter() {
        return StringUtils.isNotBlank(userUuid)
                || (userUuids != null && !userUuids.isEmpty())
                || StringUtils.isNotBlank(propertyId)
                || (propertyIds != null && !propertyIds.isEmpty())
                || StringUtils.isNotBlank(tenantId)
                || StringUtils.isNotBlank(gramPanchayat)
                || StringUtils.isNotBlank(villageName)
                || StringUtils.isNotBlank(surveyNumber);
    }
}
