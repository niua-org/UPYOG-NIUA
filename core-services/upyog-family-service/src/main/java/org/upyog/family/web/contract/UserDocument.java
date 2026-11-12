package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Universal document representation supporting verification proofs for citizen profiles,
 * properties, income, and vehicle assets in UPYOG.
 *
 * <p>Standardizes document metadata across multiple domains using {@code referenceType}
 * (e.g. {@code CITIZEN_PROFILE}, {@code PROPERTY}, {@code INCOME}, {@code VEHICLE}).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {

    /**
     * Unique internal UUID primary key for the document record.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Document type code (e.g. {@code IDENTITY_PROOF}, {@code CASTE_CERTIFICATE}, {@code SALARY_PROOF}).
     */
    @JsonProperty("documentType")
    private String documentType;

    /**
     * FileStore binary reference ID in {@code egov-filestore}.
     */
    @JsonProperty("filestoreId")
    private String filestoreId;

    /**
     * Official document identifier or certificate serial number (e.g. masked Aadhaar, OBC certificate no).
     */
    @JsonProperty("documentUid")
    private String documentUid;

    /**
     * Original upload filename (e.g. {@code aadhaar_card.pdf}).
     */
    @JsonProperty("fileName")
    private String fileName;

    /**
     * Domain classification: {@code CITIZEN_PROFILE}, {@code PROPERTY}, {@code INCOME}, or {@code VEHICLE}.
     */
    @JsonProperty("referenceType")
    private String referenceType;

    /**
     * Primary key or business identifier of the target domain entity linked to this document.
     */
    @JsonProperty("referenceId")
    private String referenceId;

    /**
     * Active state flag indicating whether the document proof remains valid.
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * Dynamic extensible JSON metadata (e.g. OCR verification scores, issuing authority, expiry date).
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Standard UPYOG audit metadata tracking creation and modification actor UUIDs and timestamps.
     */
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
