package org.egov.user.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.Address;

import java.util.List;

/**
 * Standard response envelope for citizen address operations.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Response envelope containing citizen address details")
@AllArgsConstructor
@Getter
public class AddressResponse {

    @Schema(description = "Standard UPYOG ResponseInfo contract")
    @JsonProperty("responseInfo")
    private ResponseInfo responseInfo;

    @Schema(description = "List of returned address records")
    @JsonProperty("address")
    private List<Address> address;
}
