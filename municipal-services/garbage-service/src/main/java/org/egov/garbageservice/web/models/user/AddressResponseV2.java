package org.egov.garbageservice.web.models.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AddressResponseV2 {

    @JsonProperty("responseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("address")
    private List<AddressV2> address;
}
