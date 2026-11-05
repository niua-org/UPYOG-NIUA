package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.tracer.annotations.CustomSafeHtml;

/**
 * Domain model for garbage application supporting documents (persisted in ug_gc_document).
 * Minimal schema: documentDetailId, garbageId, documentType, fileStoreId, auditDetails.
 */
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageDocument {

    @CustomSafeHtml
    @JsonProperty("documentDetailId")
    private String documentDetailId;

    @JsonProperty("garbageId")
    private Long garbageId;

    @CustomSafeHtml
    @JsonProperty("documentType")
    private String documentType;

    @CustomSafeHtml
    @JsonProperty("fileStoreId")
    private String fileStoreId;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;

    // Backward compatibility aliases
    public String getId() {
        return documentDetailId;
    }

    public void setId(String id) {
        this.documentDetailId = id;
    }

    public String getUuid() {
        return documentDetailId;
    }

    public void setUuid(String uuid) {
        this.documentDetailId = uuid;
    }

    public String getDocumentUid() {
        return fileStoreId;
    }

    public void setDocumentUid(String documentUid) {
        if (this.fileStoreId == null) {
            this.fileStoreId = documentUid;
        }
    }
}
