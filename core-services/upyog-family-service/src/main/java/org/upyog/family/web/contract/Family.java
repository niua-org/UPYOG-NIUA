package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.upyog.family.domain.model.FamilyStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Core Family registry aggregate model in UPYOG.
 *
 * <p>Represents an entire household unit within the platform, encapsulating the assigned Head of Family (HoF),
 * member roster, verification documents, and audit trails. Mapped directly to the {@code ug_family} database table.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Family {

    /**
     * Unique internal UUID primary key for the family record in {@code ug_family}.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Human-readable, globally unique business identifier formatted via {@code idgen}
     * (e.g. {@code FAM-2026-0001}). Enforced with a global UNIQUE constraint.
     */
    @JsonProperty("familyId")
    private String familyId;

    /**
     * Tenant identifier scoping this family to a specific Urban Local Body (ULB) or State (e.g. {@code pg.citya}).
     */
    @NotBlank(message = "tenantId is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * The citizen {@code uuid} of the current designated Head of Family (HoF).
     * Must correspond to an active member within {@link #members} where {@code isHead = true}.
     */
    @NotBlank(message = "headUserUuid is mandatory")
    @JsonProperty("headUserUuid")
    private String headUserUuid;

    /**
     * Display name of the designated Head of Family, dynamically enriched from citizen demographics.
     */
    @JsonProperty("headName")
    private String headName;

    /**
     * Socio-economic category classification of the household (e.g. {@code GENERAL}, {@code BPL}, {@code EWS}).
     */
    @JsonProperty("category")
    private String category;

    /**
     * Operational lifecycle state of the family unit (e.g. {@link FamilyStatus#ACTIVE}, {@link FamilyStatus#INACTIVE}, {@link FamilyStatus#MERGED}, {@link FamilyStatus#SPLIT}).
     */
    @JsonProperty("status")
    private FamilyStatus status;

    /**
     * Workflow application status tracking the statutory approval lifecycle of the family registration application
     * (e.g. {@code PENDING_FOR_APPROVAL}, {@code APPROVED}, {@code REJECTED}).
     */
    @JsonProperty("applicationStatus")
    private String applicationStatus;

    /**
     * Unique, formatted application tracking identifier assigned at household creation time via the UPYOG IdGen service
     * using the {@code family.application.num} naming format (e.g. {@code FAM-APP-2026-000001}).
     */
    @JsonProperty("applicationNo")
    private String applicationNo;

    /**
     * Unique identifier (UUID or username) of the municipal officer or system actor who approved the family registration
     * during the workflow {@code APPROVE} transition.
     */
    @JsonProperty("approvedBy")
    private String approvedBy;

    /**
     * Epoch timestamp in milliseconds recording the exact point in time when the family application transitioned to {@code APPROVED} state.
     */
    @JsonProperty("approvalDate")
    private Long approvalDate;

    /**
     * Unique identifier representing the digitally issued or external statutory Family Certificate associated with this verified household.
     */
    @JsonProperty("familyCertificateId")
    private String familyCertificateId;

    /**
     * Property ID representing the residential address property linked to this household unit.
     */
    @JsonProperty("familyAddressPropertyId")
    private String familyAddressPropertyId;

    /**
     * Originating portal or department source of family declaration (e.g. {@code CITIZEN_PORTAL}, {@code CSC}).
     */
    @JsonProperty("familySource")
    private String familySource;

    /**
     * Identifies the type of family unit: UPYOG_INTERNAL (native) or STATE_REGISTRY (state-synced e.g. Haryana PPP).
     */
    @JsonProperty("familyType")
    private String familyType;

    /**
     * Dynamic extensible JSON metadata (e.g. address details, state sync identifiers, ration card numbers).
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Roster of individual citizen members affiliated with this family unit.
     * Must contain at least one active member (the Head of Family).
     */
    @Valid
    @NotEmpty(message = "Family must contain at least one member (Head of Family)")
    @JsonProperty("members")
    @Builder.Default
    private List<FamilyMember> members = new ArrayList<>();

    /**
     * Verification documents and address proofs associated with the family unit.
     */
    @Valid
    @JsonProperty("documents")
    @Builder.Default
    private List<FamilyDocument> documents = new ArrayList<>();

    /**
     * Standard UPYOG audit metadata recording creation and last-modification actor UUIDs and epoch timestamps.
     */
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;

    /**
     * Workflow transition details associated with this family unit.
     */
    @Valid
    @JsonProperty("workflow")
    private Workflow workflow;

    /**
     * Enriches the headName attribute based on the designated Head of Family's resolved member name.
     */
    public void enrichHeadName() {
        if (StringUtils.isNotBlank(this.headUserUuid) && this.members != null) {
            for (FamilyMember m : this.members) {
                if (this.headUserUuid.equals(m.getUserUuid())) {
                    if (StringUtils.isNotBlank(m.getName())) {
                        this.headName = m.getName();
                    } else if (m.getCitizenDetails() != null && StringUtils.isNotBlank(m.getCitizenDetails().getName())) {
                        this.headName = m.getCitizenDetails().getName();
                    }
                    break;
                }
            }
        }
    }
}
