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
import org.egov.user.domain.model.Address;

/**
 * Request payload for creating, updating, or deleting citizen addresses.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Request envelope for citizen address CRUD operations")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AddressRequest {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "Address domain entity details")
    @JsonProperty("address")
    private Address address;

    @Schema(description = "Citizen UUID associated with this address", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @JsonProperty("uuid")
    @JsonAlias({"userUuid", "user_uuid"})
    private String uuid;

    @Schema(description = "Unique address record ID (required for update/delete)", example = "addr-9876-uuid")
    @JsonProperty("addressId")
    private String addressId;

    @JsonIgnore
    public String getUserUuid() {
        return this.uuid;
    }

    public void setUserUuid(String userUuid) {
        this.uuid = userUuid;
    }

    public static class AddressRequestBuilder {
        public AddressRequestBuilder userUuid(String userUuid) {
            this.uuid = userUuid;
            return this;
        }
    }
}
