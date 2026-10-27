package org.upyog.family.web.contract.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Address details for State Family registry integration (UC2).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateAddress {

    @JsonProperty("doorNo")
    private String doorNo;

    @JsonProperty("houseNumber")
    private String houseNumber;

    @JsonProperty("streetName")
    private String streetName;

    @JsonProperty("addressLine1")
    private String addressLine1;

    @JsonProperty("addressLine2")
    private String addressLine2;

    @JsonProperty("landmark")
    private String landmark;

    @JsonProperty("city")
    private String city;

    @JsonProperty("locality")
    private String locality;

    @JsonProperty("pinCode")
    private String pinCode;

    @JsonProperty("district")
    private String district;

    @JsonProperty("state")
    private String state;
}
