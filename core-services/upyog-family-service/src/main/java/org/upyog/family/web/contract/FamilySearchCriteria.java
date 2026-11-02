package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.upyog.family.domain.model.FamilyStatus;

import java.util.List;

/**
 * Criteria object for searching families across tenants, head, member UUIDs, or mobile numbers in UPYOG.
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

    @JsonProperty("familyId")
    private String familyId;

    @JsonProperty("familyIds")
    private List<String> familyIds;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("headUserUuid")
    private String headUserUuid;

    @JsonProperty("memberUserUuid")
    private String memberUserUuid;

    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @JsonProperty("category")
    private String category;

    @JsonProperty("status")
    private FamilyStatus status;

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("limit")
    private Integer limit;

    /**
     * Checks if all query filter parameters are null or empty.
     *
     * @return true if no query filters are specified; false otherwise
     */
    public boolean isEmpty() {
        return (familyId == null || familyId.trim().isEmpty())
                && (familyIds == null || familyIds.isEmpty())
                && (tenantId == null || tenantId.trim().isEmpty())
                && (headUserUuid == null || headUserUuid.trim().isEmpty())
                && (memberUserUuid == null || memberUserUuid.trim().isEmpty())
                && (mobileNumber == null || mobileNumber.trim().isEmpty())
                && (category == null || category.trim().isEmpty())
                && status == null;
    }
}
