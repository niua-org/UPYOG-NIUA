package org.egov.user.web.contract.document;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.UserDocument;

import java.util.List;

/**
 * Request DTO for creating, updating, or deleting citizen documents.
 */
@Schema(description = "Request envelope for citizen document CRUD operations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocumentRequest {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "List of document domain entities to attach/modify")
    @Valid
    @JsonProperty("documents")
    private List<UserDocument> documents;

    @Schema(description = "Single document domain entity (convenience field)")
    @Valid
    @JsonProperty("document")
    private UserDocument document;

    @Schema(description = "Citizen UUID associated with these documents", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @JsonProperty("uuid")
    @JsonAlias({"userUuid", "user_uuid"})
    private String uuid;

    @Schema(description = "Internal user numeric ID", example = "1024")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long userId;

    @Schema(description = "Tenant identifier", example = "pg.citya")
    private String tenantId;

    @Schema(description = "Unique document record ID (required for update/delete)", example = "doc-9876-uuid")
    private String documentId;

    /**
     * Compatibility getter for {@code userUuid} mapping to {@code uuid}.
     *
     * @return the user UUID string
     */
    @JsonIgnore
    public String getUserUuid() {
        return this.uuid;
    }

    /**
     * Compatibility setter for {@code userUuid} mapping to {@code uuid}.
     *
     * @param userUuid the user UUID string
     */
    public void setUserUuid(String userUuid) {
        this.uuid = userUuid;
    }

    /**
     * Custom builder extensions for {@link UserDocumentRequest}.
     */
    public static class UserDocumentRequestBuilder {
        /**
         * Sets {@code uuid} via alias {@code userUuid}.
         *
         * @param userUuid the user UUID string
         * @return builder instance
         */
        public UserDocumentRequestBuilder userUuid(String userUuid) {
            this.uuid = userUuid;
            return this;
        }
    }

    /**
     * Resolves effective documents from either plural list or singular property.
     *
     * @return immutable or safe list of documents to process
     */
    @JsonIgnore
    public List<UserDocument> getEffectiveDocuments() {
        if (documents != null && !documents.isEmpty()) {
            return documents;
        }
        if (document != null) {
            return List.of(document);
        }
        return List.of();
    }
}
