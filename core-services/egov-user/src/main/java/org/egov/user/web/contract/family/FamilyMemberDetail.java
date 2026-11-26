package org.egov.user.web.contract.family;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.web.contract.UserSearchResponseContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed family member payload encapsulating membership metadata, citizen profile,
 * and declared 3NF assets (vehicles, income, properties).
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FamilyMemberDetail {

    @JsonProperty("id")
    private String id;

    @JsonProperty("familyId")
    private String familyId;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("userUuid")
    private String userUuid;

    @JsonProperty("relationshipWithHead")
    private String relationshipWithHead;

    @JsonProperty("isHead")
    private Boolean isHead;

    @JsonProperty("isVerified")
    private Boolean isVerified;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("status")
    private String status;

    @JsonProperty("profile")
    private UserSearchResponseContent profile;

    @JsonProperty("vehicles")
    @Builder.Default
    private List<UserVehicle> vehicles = new ArrayList<>();

    @JsonProperty("income")
    private UserIncome income;

    @JsonProperty("properties")
    @Builder.Default
    private List<UserProperty> properties = new ArrayList<>();
}
