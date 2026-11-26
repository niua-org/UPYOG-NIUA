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
import java.util.List;

/**
 * Request payload for a citizen requesting to join an existing Family Unit in UPYOG.
 *
 * <p>Supports demographic enrichment via {@link CitizenProfile} and asset declarations
 * (properties, income, vehicles) adhering to 3NF architecture.</p>
 *
 * <p>Requires authorization consent from the current Head of Family (HoF).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinFamilyRequest {

    /**
     * Standard platform request metadata envelope containing caller credentials and transaction headers.
     */
    @NotNull(message = "RequestInfo cannot be null")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Business identifier of the target Family Unit to join (e.g. {@code FAM-2026-0001}).
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
     * Citizen {@code uuid} of the applicant requesting to join.
     */
    @JsonProperty("applicantUserUuid")
    private String applicantUserUuid;

    /**
     * Alternate attribute for applicant citizen {@code uuid} for backward compatibility.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Relationship with the Head of Family, dynamically validated against MDMS.
     * <p>Optional at the root if supplied inside {@link #member}. Validated in JoinFamilyValidator.</p>
     */
    @JsonProperty("relationshipWithHead")
    private String relationshipWithHead;

    /**
     * Transaction identifier representing verified HoF authorization consent.
     * <p>Optional at the root if supplied inside {@link #member}. Validated in JoinFamilyValidator.</p>
     */
    @JsonProperty("txnId")
    private String txnId;

    /**
     * Optional nested member envelope containing full member declarations.
     * <p>Added to align with the createFamily / addMember payload refactor. Clients can submit the joining
     * citizen's details either as this nested {@link FamilyMember} object or as direct top-level fields.</p>
     */
    @Valid
    @JsonProperty("member")
    private FamilyMember member;

    /**
     * Enriched citizen demographic profile and identity proof documents.
     * <p>Acts as the single source of truth for citizen demographics (name, mobile, gender, dob, photo,
     * caste, divyang status) adhering to 3NF architecture and avoiding field duplication at the outer layer.</p>
     */
    @Valid
    @JsonProperty("citizenDetails")
    private CitizenProfile citizenDetails;

    /**
     * Declared property assets (Rural landholdings and Urban municipal properties).
     * <p>Optional list matching the createFamily asset declaration structure, synchronized to egov-user.</p>
     */
    @Valid
    @JsonProperty("properties")
    @Builder.Default
    private List<UserProperty> properties = new ArrayList<>();

    /**
     * Declared income, employment taxonomy, and salary/tax proof documents.
     * <p>Optional income declaration model matching the createFamily member asset structure.</p>
     */
    @Valid
    @JsonProperty("income")
    private UserIncomeDetail income;

    /**
     * Declared motor vehicles, VAHAN classifications, and RC documents.
     * <p>Optional vehicle declarations matching the createFamily member asset structure.</p>
     */
    @Valid
    @JsonProperty("vehicles")
    @Builder.Default
    private List<UserVehicleDetail> vehicles = new ArrayList<>();

    /**
     * Resolves and retrieves the applicant's user UUID from either applicantUserUuid, userUuid, or nested member.
     * <p>Ensures 100% backward compatibility with legacy payloads that sent applicantUserUuid or userUuid,
     * as well as newer clients supplying it inside the member envelope.</p>
     *
     * @return applicant user UUID, preferring applicantUserUuid if present, then userUuid, then member.getUserUuid()
     */
    public String getApplicantUserUuid() {
        if (StringUtils.isNotBlank(applicantUserUuid)) {
            return applicantUserUuid;
        }
        if (StringUtils.isNotBlank(userUuid)) {
            return userUuid;
        }
        return member != null ? member.getUserUuid() : null;
    }

    /**
     * Resolves relationship with head, falling back to member.
     * <p>Supports relationship code specified either at top level or nested within the member object.</p>
     *
     * @return relationship with head
     */
    public String getRelationshipWithHead() {
        if (StringUtils.isNotBlank(relationshipWithHead)) {
            return relationshipWithHead;
        }
        return member != null ? member.getRelationshipWithHead() : null;
    }

    /**
     * Resolves transaction identifier (consent token), falling back to member.
     * <p>Supports transaction identifier specified either at top level or nested within the member object.</p>
     *
     * @return transaction identifier
     */
    public String getTxnId() {
        if (StringUtils.isNotBlank(txnId)) {
            return txnId;
        }
        return member != null ? member.getConsentToken() : null;
    }

    /**
     * Resolves citizenDetails, falling back to member.
     * <p>Retrieves citizen demographic profile either from the direct attribute or the nested member envelope.</p>
     *
     * @return citizen profile details
     */
    public CitizenProfile getCitizenDetails() {
        if (citizenDetails != null) {
            return citizenDetails;
        }
        return member != null ? member.getCitizenDetails() : null;
    }

    /**
     * Resolves declared properties, falling back to member.
     * <p>Returns declared properties from direct attribute if present, otherwise delegates to nested member.</p>
     *
     * @return list of user properties
     */
    public List<UserProperty> getProperties() {
        if (properties != null && !properties.isEmpty()) {
            return properties;
        }
        return member != null ? member.getProperties() : properties;
    }

    /**
     * Resolves declared income, falling back to member.
     * <p>Returns income details from direct attribute if present, otherwise delegates to nested member.</p>
     *
     * @return user income details
     */
    public UserIncomeDetail getIncome() {
        if (income != null) {
            return income;
        }
        return member != null ? member.getIncome() : null;
    }

    /**
     * Resolves declared vehicles, falling back to member.
     * <p>Returns vehicle declarations from direct attribute if present, otherwise delegates to nested member.</p>
     *
     * @return list of user vehicles
     */
    public List<UserVehicleDetail> getVehicles() {
        if (vehicles != null && !vehicles.isEmpty()) {
            return vehicles;
        }
        return member != null ? member.getVehicles() : vehicles;
    }
}
