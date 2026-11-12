package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.upyog.family.domain.model.FamilyStatus;

import java.util.List;

/**
 * Criteria object for searching families across tenants, head, member UUIDs, or mobile numbers in UPYOG.
 *
 * <p>Encapsulates all supported query parameters consumed by {@link org.upyog.family.domain.service.FamilyService#searchFamilies}
 * and {@link org.upyog.family.persistence.repository.FamilyRepository#search}.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilySearchCriteria {

    /**
     * Exact single family business identifier filter (e.g. {@code FAM-2026-0001}).
     */
    @JsonProperty("familyId")
    private String familyId;

    /**
     * List of family identifiers for bulk query resolution.
     */
    @JsonProperty("familyIds")
    private List<String> familyIds;

    /**
     * Mandated tenant identifier scoping the search (e.g. {@code pg.citya}).
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Filter households by the Head of Family citizen UUID.
     */
    @JsonProperty("headUserUuid")
    private String headUserUuid;

    /**
     * Filter households that contain the specified citizen UUID as an active member.
     */
    @JsonProperty("memberUserUuid")
    private String memberUserUuid;

    @JsonProperty("memberUserUuids")
    private List<String> memberUserUuids;

    /**
     * Filter households by citizen registered mobile number (resolved via {@code egov-user}).
     */
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    /**
     * Socio-economic category filter (e.g. {@code GENERAL}, {@code BPL}).
     */
    @JsonProperty("category")
    private String category;

    /**
     * Operational state filter (e.g. {@link FamilyStatus#ACTIVE}).
     */
    @JsonProperty("status")
    private FamilyStatus status;

    /**
     * Filter households by family type (e.g. {@code UPYOG_INTERNAL}, {@code STATE_REGISTRY}).
     */
    @JsonProperty("familyType")
    private String familyType;

    /**
     * Pagination offset index for SQL query.
     */
    @JsonProperty("offset")
    private Integer offset;

    /**
     * Maximum number of family records to retrieve per page.
     */
    @JsonProperty("limit")
    private Integer limit;

    /**
     * Checks if all query filter parameters are null or empty.
     *
     * @return true if no query filters are specified; false otherwise
     */
    public boolean isEmpty() {
        boolean hasIdent = StringUtils.isNotBlank(familyId) || !CollectionUtils.isEmpty(familyIds) || StringUtils.isNotBlank(tenantId);
        boolean hasMembers = StringUtils.isNotBlank(headUserUuid) || StringUtils.isNotBlank(memberUserUuid)
                || !CollectionUtils.isEmpty(memberUserUuids) || StringUtils.isNotBlank(mobileNumber);
        return !hasIdent && !hasMembers && StringUtils.isBlank(category) && status == null && StringUtils.isBlank(familyType);
    }
}
