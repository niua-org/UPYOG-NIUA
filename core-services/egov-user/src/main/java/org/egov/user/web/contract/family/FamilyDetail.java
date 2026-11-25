package org.egov.user.web.contract.family;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed family unit aggregate encapsulating household attributes and enriched member details.
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
public class FamilyDetail {

    @JsonProperty("id")
    private String id;

    @JsonProperty("familyId")
    private String familyId;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("headUserUuid")
    private String headUserUuid;

    @JsonProperty("headName")
    private String headName;

    @JsonProperty("category")
    private String category;

    @JsonProperty("status")
    private String status;

    @JsonProperty("applicationStatus")
    private String applicationStatus;

    @JsonProperty("applicationNo")
    private String applicationNo;

    @JsonProperty("familyAddressPropertyId")
    private String familyAddressPropertyId;

    @JsonProperty("familySource")
    private String familySource;

    @JsonProperty("members")
    @Builder.Default
    private List<FamilyMemberDetail> members = new ArrayList<>();
}
