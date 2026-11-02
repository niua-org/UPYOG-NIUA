package org.egov.user.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.egov.user.domain.model.enums.AddressType;

/**
 * Criteria DTO for filtering and searching address entities in the repository.
 */
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class AddressSearchCriteria {
    private Long id;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long userId;
    private String tenantId;
    private AddressType addressType;
    private String city;
    private String pinCode;
    private String status;

    @JsonProperty("uuid")
    @JsonAlias({"userUuid", "user_uuid"})
    private String uuid; // Search address with particular user using uuid

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
     * Normalizes status to lowercase.
     *
     * @param status status string
     */
    public void setStatus(String status) {
        this.status = status != null ? status.trim().toLowerCase() : null;
    }

    /**
     * Custom builder extensions for {@link AddressSearchCriteria}.
     */
    public static class AddressSearchCriteriaBuilder {
        /**
         * Sets {@code uuid} via alias {@code userUuid}.
         *
         * @param userUuid the user UUID string
         * @return builder instance
         */
        public AddressSearchCriteriaBuilder userUuid(String userUuid) {
            this.uuid = userUuid;
            return this;
        }

        /**
         * Sets normalized lowercase status.
         *
         * @param status address status
         * @return builder instance
         */
        public AddressSearchCriteriaBuilder status(String status) {
            this.status = status != null ? status.trim().toLowerCase() : null;
            return this;
        }
    }
}
