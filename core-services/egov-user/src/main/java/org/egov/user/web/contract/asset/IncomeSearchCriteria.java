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
 * Search criteria contract for querying citizen income declarations from {@code ug_user_income}.
 *
 * <p>Supports both flat and nested payloads matching standard UPYOG criteria conventions.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Search criteria envelope for querying citizen income declarations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IncomeSearchCriteria {

    /**
     * Standard UPYOG RequestInfo contract.
     */
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Nested criteria wrapper for clients wrapping filters inside {@code criteria}.
     */
    @JsonProperty("criteria")
    private IncomeSearchCriteria criteria;

    /**
     * Citizen {@code uuid} owning the income record.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * List of citizen UUIDs to filter income declarations across multiple household members.
     */
    @JsonProperty("userUuids")
    private List<String> userUuids;

    /**
     * Unique income primary key ID.
     */
    @JsonProperty("incomeId")
    private String incomeId;

    /**
     * Alternate unique income ID alias.
     */
    @JsonProperty("id")
    private String id;

    /**
     * List of income primary key IDs.
     */
    @JsonProperty("incomeIds")
    private List<String> incomeIds;

    /**
     * Tenant identifier scoping the income record.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Occupation classification code filter.
     */
    @JsonProperty("occupation")
    private String occupation;

    /**
     * Fiscal identifier type filter (e.g. {@code PAN}, {@code TAN}, or {@code GSTIN}).
     */
    @JsonProperty("identifierType")
    private String identifierType;

    /**
     * Fiscal identifier string value (e.g. PAN number).
     */
    @JsonProperty("identifierValue")
    private String identifierValue;

    /**
     * Lifecycle status filter (e.g. {@code ACTIVE}, {@code INACTIVE}, or {@code ALL}).
     * Defaults to {@code ACTIVE} when null or blank.
     */
    @JsonProperty("status")
    private String status;

    /**
     * Flag indicating whether inactive/deactivated income records should be included.
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
     * Resolves income primary key ID from {@code incomeId} or {@code id}.
     *
     * @return effective income ID string
     */
    public String resolveIncomeId() {
        if (StringUtils.isNotBlank(this.incomeId)) {
            return this.incomeId.trim();
        }
        if (StringUtils.isNotBlank(this.id)) {
            return this.id.trim();
        }
        return null;
    }

    /**
     * Unwraps nested criteria if passed inside {@code criteria} property.
     *
     * @return normalized effective search criteria
     */
    public IncomeSearchCriteria resolveCriteria() {
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
                || StringUtils.isNotBlank(incomeId)
                || StringUtils.isNotBlank(id)
                || (incomeIds != null && !incomeIds.isEmpty())
                || StringUtils.isNotBlank(tenantId)
                || StringUtils.isNotBlank(occupation)
                || StringUtils.isNotBlank(identifierType)
                || StringUtils.isNotBlank(identifierValue);
    }
}
