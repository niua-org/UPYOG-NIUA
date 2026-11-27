package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Request payload for adding a new member to an existing Family Unit in UPYOG.
 *
 * <p>Supports demographic enrichment via {@link CitizenProfile} and asset declarations
 * (properties, income, vehicles) adhering to 3NF architecture. Aligns with the Parivar
 * Pehchan Patra (PPP) payload format, allowing declarations either at the root level or nested
 * within the {@link FamilyMember} envelope.</p>
 *
 * <p>Supports real-time Aadhaar OTP consent verification for adult members.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {

    /**
     * Standard platform request metadata envelope containing caller credentials and transaction headers.
     */
    @NotNull(message = "RequestInfo cannot be null")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Business identifier of the target Family Unit (e.g. {@code FAM-2026-0001}).
     */
    @NotBlank(message = "Family ID is mandatory")
    @JsonProperty("familyId")
    private String familyId;

    /**
     * Tenant identifier scoping the target family (e.g. {@code pg.citya}).
     */
    @NotBlank(message = "Tenant ID is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Optional citizen {@code uuid} if supplied at root level.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Relationship with the Head of Family, dynamically validated against MDMS.
     */
    @JsonProperty("relationshipWithHead")
    private String relationshipWithHead;

    /**
     * Optional nested member envelope containing full member declarations.
     */
    @Valid
    @JsonProperty("member")
    private FamilyMember member;

    /**
     * Enriched citizen demographic profile and identity proof documents adhering to 3NF.
     */
    @Valid
    @JsonProperty("citizenDetails")
    private CitizenProfile citizenDetails;

    /**
     * Declared property assets (Rural landholdings and Urban municipal properties).
     */
    @Valid
    @JsonProperty("properties")
    @Builder.Default
    private List<UserProperty> properties = new ArrayList<>();

    /**
     * Declared income, employment taxonomy, and salary/tax proof documents.
     */
    @Valid
    @JsonProperty("income")
    private UserIncomeDetail income;

    /**
     * Declared motor vehicles, VAHAN classifications, and RC documents.
     */
    @Valid
    @JsonProperty("vehicles")
    @Builder.Default
    private List<UserVehicleDetail> vehicles = new ArrayList<>();

    /**
     * Aadhaar OTP consent token validating association with the family unit.
     */
    @JsonProperty("consentToken")
    private String consentToken;

    /**
     * Resolves the target citizen user UUID from either root userUuid or nested member.
     *
     * @return citizen user UUID
     */
    public String getUserUuid() {
        if (StringUtils.isNotBlank(userUuid)) {
            return userUuid;
        }
        return member != null ? member.getUserUuid() : null;
    }

    /**
     * Resolves relationship with head, falling back to nested member envelope.
     *
     * @return relationship code
     */
    public String getRelationshipWithHead() {
        if (StringUtils.isNotBlank(relationshipWithHead)) {
            return relationshipWithHead;
        }
        return member != null ? member.getRelationshipWithHead() : null;
    }

    /**
     * Resolves the citizen demographic details, preferring top-level declaration if present.
     *
     * @return citizen profile demographic model
     */
    public CitizenProfile getCitizenDetails() {
        if (citizenDetails != null) {
            return citizenDetails;
        }
        return member != null ? member.getCitizenDetails() : null;
    }

    /**
     * Resolves declared property assets, preferring top-level list if present and non-empty.
     *
     * @return declared property assets
     */
    public List<UserProperty> getProperties() {
        if (properties != null && !properties.isEmpty()) {
            return properties;
        }
        return member != null && member.getProperties() != null
                ? member.getProperties()
                : Collections.emptyList();
    }

    /**
     * Resolves declared income, preferring top-level declaration if present.
     *
     * @return declared income model
     */
    public UserIncomeDetail getIncome() {
        if (income != null) {
            return income;
        }
        return member != null ? member.getIncome() : null;
    }

    /**
     * Resolves declared motor vehicles, preferring top-level list if present and non-empty.
     *
     * @return declared vehicle list
     */
    public List<UserVehicleDetail> getVehicles() {
        if (vehicles != null && !vehicles.isEmpty()) {
            return vehicles;
        }
        return member != null && member.getVehicles() != null
                ? member.getVehicles()
                : Collections.emptyList();
    }

    /**
     * Resolves the effective FamilyMember envelope, initializing a default instance if absent.
     *
     * @return effective FamilyMember instance
     */
    public FamilyMember getMember() {
        if (member != null) {
            return member;
        }
        member = FamilyMember.builder()
                .userUuid(userUuid)
                .relationshipWithHead(relationshipWithHead)
                .citizenDetails(citizenDetails)
                .properties(properties != null ? properties : new ArrayList<>())
                .income(income)
                .vehicles(vehicles != null ? vehicles : new ArrayList<>())
                .consentToken(consentToken)
                .build();
        return member;
    }
}
