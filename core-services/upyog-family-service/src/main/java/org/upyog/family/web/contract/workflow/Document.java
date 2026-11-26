package org.upyog.family.web.contract.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;
import org.upyog.family.web.contract.AuditDetails;

import java.util.Objects;

/**
 * Object holding details of documents attached during workflow transitions.
 */
@Schema(description = "Object holding details of documents attached during workflow transitions.")
@Validated
@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @JsonProperty("id")
    @Size(max = 64)
    private String id;

    @JsonProperty("documentType")
    private String documentType;

    @JsonProperty("fileStoreId")
    private String fileStoreId;

    @JsonProperty("documentUid")
    @Size(max = 64)
    private String documentUid;

    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    @JsonProperty("auditDetails")
    @Valid
    private AuditDetails auditDetails;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id) &&
                Objects.equals(documentType, document.documentType) &&
                Objects.equals(fileStoreId, document.fileStoreId) &&
                Objects.equals(documentUid, document.documentUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, documentType, fileStoreId, documentUid);
    }
}
