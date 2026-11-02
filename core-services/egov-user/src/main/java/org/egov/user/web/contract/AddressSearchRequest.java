package org.egov.user.web.contract;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.enums.AddressType;

/**
 * Request DTO for searching addresses.
 */
@Schema(description = "Request envelope for searching citizen addresses")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AddressSearchRequest {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "Citizen UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @JsonProperty("uuid")
    @JsonAlias({"userUuid", "user_uuid"})
    private String uuid;

    @Schema(description = "Internal user numeric ID", example = "1024")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long userId;

    @Schema(description = "Tenant identifier", example = "pg.citya")
    private String tenantId;

    @Schema(description = "Address type filter: PERMANENT or CORRESPONDENCE", example = "PERMANENT")
    private AddressType addressType;

    @Schema(description = "City / Municipality name", example = "City A")
    private String city;

    @Schema(description = "Postal pincode", example = "143001")
    private String pinCode;

    @Schema(description = "Unique numeric address ID", example = "501")
    private Long id;

    @Schema(description = "Address status: active or inactive", example = "active")
    private String status;

    @Schema(description = "Nested AddressSearchCriteria payload (if provided)")
    @JsonProperty("AddressSearchCriteria")
    @JsonAlias({"criteria", "addressSearchCriteria"})
    private AddressSearchCriteria criteria;

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
     * Custom setter ensuring status is lower-cased and trimmed.
     *
     * @param status address status string
     */
    public void setStatus(String status) {
        this.status = status != null ? status.trim().toLowerCase() : null;
    }

    /**
     * Custom builder extensions for {@link AddressSearchRequest}.
     */
    public static class AddressSearchRequestBuilder {
        /**
         * Sets {@code uuid} via alias {@code userUuid}.
         *
         * @param userUuid user UUID string
         * @return builder instance
         */
        public AddressSearchRequestBuilder userUuid(String userUuid) {
            this.uuid = userUuid;
            return this;
        }

        /**
         * Sets status normalized to lowercase.
         *
         * @param status address status
         * @return builder instance
         */
        public AddressSearchRequestBuilder status(String status) {
            this.status = status != null ? status.trim().toLowerCase() : null;
            return this;
        }
    }
}
