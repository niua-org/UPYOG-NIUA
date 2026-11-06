package org.egov.garbageservice.web.models.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.web.models.garbage.ApplicantDetail;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class AddressRequestV2 {

    @JsonProperty("requestInfo")
    private RequestInfo requestInfo;

    @JsonProperty("address")
    private AddressV2 address;

    @JsonProperty("applicantDetails")
    private ApplicantDetail applicantDetails;

    @JsonProperty("userUuid")
    private String userUuid;

    @JsonProperty("addressId")
    private String addressId;
}
