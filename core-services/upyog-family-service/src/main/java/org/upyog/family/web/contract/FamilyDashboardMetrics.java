package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Household summary metrics model for family dashboard presentation in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDashboardMetrics {

    @JsonProperty("familyId")
    private String familyId;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("familyStatus")
    private String familyStatus;

    @JsonProperty("headUserUuid")
    private String headUserUuid;

    @JsonProperty("headName")
    private String headName;

    @JsonProperty("totalMembersCount")
    private int totalMembersCount;

    @JsonProperty("activeMembersCount")
    private int activeMembersCount;

    @JsonProperty("verifiedMembersCount")
    private int verifiedMembersCount;

    @JsonProperty("documentsCount")
    private int documentsCount;

    @JsonProperty("lineageEventsCount")
    private int lineageEventsCount;

    @JsonProperty("activeMembers")
    private List<FamilyMember> activeMembers;

    @JsonProperty("recentLineage")
    private List<FamilyLineage> recentLineage;
}
