package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contract representing an attached proof document in UPYOG (Address proof, BPL certificate, HoF consent).
 *
 * <p>Maps to the {@code ug_family_document} table referencing files persisted in UPYOG FileStore.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDocument {

    /**
     * Unique internal UUID primary key for the document record in {@code ug_family_document}.
     */
    @JsonProperty("id")
    private String id;

    /**
     * The unique business identifier of the associated family unit (e.g. {@code FAM-2026-0001}).
     */
    @JsonProperty("familyId")
    private String familyId;

    /**
     * Tenant identifier scoping the document (e.g. {@code pg.citya}).
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Category of the document proof (e.g. {@code ADDRESS_PROOF}, {@code BPL_CERTIFICATE}, {@code RATION_CARD}).
     */
    @NotBlank(message = "Document type is mandatory")
    @JsonProperty("documentType")
    private String documentType;

    /**
     * FileStore ID referencing the binary file stored in the UPYOG FileStore service.
     */
    @NotBlank(message = "FileStore ID is mandatory")
    @JsonProperty("filestoreId")
    private String filestoreId;

    /**
     * Active state flag for the document proof (supports soft-deletion).
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * Standard UPYOG audit metadata recording creator and modification timestamps.
     */
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
