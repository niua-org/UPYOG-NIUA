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
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDocument {

    @JsonProperty("id")
    private String id;

    @JsonProperty("familyId")
    private String familyId;

    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(message = "Document type is mandatory")
    @JsonProperty("documentType")
    private String documentType;

    @NotBlank(message = "FileStore ID is mandatory")
    @JsonProperty("filestoreId")
    private String filestoreId;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
