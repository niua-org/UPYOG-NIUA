package org.egov.user.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Domain model representing a citizen identity or municipal document attached to eg_user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {

    private String id;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long userId;

    @JsonProperty("uuid")
    @JsonAlias({"userUuid", "user_uuid"})
    private String uuid;
    private String tenantId;
    private String documentType;
    private String documentUid;
    private String filestoreId;
    private String fileName;
    private Boolean isVerified;
    private Boolean active;
    private String fileStoreUrl;
    private Object additionalDetails;

    private Long createdBy;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private Date createdDate;

    private Long lastModifiedBy;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private Date lastModifiedDate;

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
     * Custom builder extensions for {@link UserDocument}.
     */
    public static class UserDocumentBuilder {
        /**
         * Sets the {@code uuid} via alias {@code userUuid}.
         *
         * @param userUuid the user UUID string
         * @return builder instance
         */
        public UserDocumentBuilder userUuid(String userUuid) {
            this.uuid = userUuid;
            return this;
        }
    }
}
