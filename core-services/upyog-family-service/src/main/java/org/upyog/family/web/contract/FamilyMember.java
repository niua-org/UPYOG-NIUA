package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.upyog.family.domain.model.ExitReason;
import org.upyog.family.domain.model.MemberStatus;

/**
 * Contract representing an individual family member mapped to a citizen account in UPYOG.
 *
 * <p>The {@code relationshipWithHead} is represented as a String resolved dynamically against
 * UPYOG MDMS master data (module 'common-masters', master 'FamilyRelationship'), enabling
 * runtime additions without requiring service redeployment.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMember {

    @JsonProperty("id")
    private String id;

    @JsonProperty("familyId")
    private String familyId;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("userUuid")
    private String userUuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("relationshipWithHead")
    private String relationshipWithHead;

    @JsonProperty("isHead")
    private Boolean isHead;

    @JsonProperty("isVerified")
    private Boolean isVerified;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("status")
    private MemberStatus status;

    @JsonProperty("exitReason")
    private ExitReason exitReason;

    @JsonProperty("exitDate")
    private Long exitDate;

    @JsonProperty("consentToken")
    private String consentToken;

    @JsonProperty("consentDate")
    private Long consentDate;

    @JsonProperty("effectiveFrom")
    private Long effectiveFrom;

    @JsonProperty("effectiveTo")
    private Long effectiveTo;

    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    @JsonProperty("citizenDetails")
    private CitizenProfile citizenDetails;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
