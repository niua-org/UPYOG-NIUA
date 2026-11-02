package org.egov.user.web.contract.document;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

import java.util.List;

/**
 * Criteria DTO for searching citizen documents.
 */
@Schema(description = "Criteria envelope for searching citizen documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocumentSearchCriteria {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "Unique document record ID", example = "doc-1234-uuid")
    private String id;

    @Schema(description = "List of document IDs")
    private List<String> ids;

    @Schema(description = "Internal user numeric ID", example = "1024")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long userId;

    @Schema(description = "Citizen UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @JsonProperty("uuid")
    @JsonAlias({"userUuid", "user_uuid"})
    private String uuid;

    @Schema(description = "Tenant identifier", example = "pg.citya")
    private String tenantId;

    @Schema(description = "Document type (e.g., AADHAAR, PAN, VOTER_ID, ELECTRICITY_BILL)", example = "AADHAAR")
    private String documentType;

    @Schema(description = "egov-filestore file ID", example = "filestore-uuid-5678")
    private String filestoreId;

    @Schema(description = "Verification status filter", example = "true")
    private Boolean isVerified;

    @Schema(description = "Active status filter (true = active only)", example = "true")
    private Boolean active;

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
     * Custom builder extensions for {@link UserDocumentSearchCriteria}.
     */
    public static class UserDocumentSearchCriteriaBuilder {
        /**
         * Sets {@code uuid} via alias {@code userUuid}.
         *
         * @param userUuid user UUID string
         * @return builder instance
         */
        public UserDocumentSearchCriteriaBuilder userUuid(String userUuid) {
            this.uuid = userUuid;
            return this;
        }
    }

    @Schema(description = "Nested criteria payload (if wrapped)")
    @JsonProperty("criteria")
    @JsonAlias({"userDocumentSearchCriteria", "UserDocumentSearchCriteria"})
    private UserDocumentSearchCriteria criteria;
}
