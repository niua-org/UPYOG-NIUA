package org.egov.garbageservice.web.models.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.egov.garbageservice.enums.AddressType;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class AddressV2 {
    private String pinCode;
    private String city;
    private String address;
    private AddressType type;
    private Long id;
    private String tenantId;
    private Long userId;
    private String addressType;
    @JsonProperty("LastModifiedBy")
    private Long lastModifiedBy;
    @JsonProperty("LastModifiedDate")
    private Date lastModifiedDate;
    private String address2;
    private String houseNumber;
    private String houseName;
    private String streetName;
    private String landmark;
    private String locality;

    public boolean isInvalid() {
        return isPinCodeInvalid() || isCityInvalid() || isAddressInvalid();
    }

    public boolean isNotEmpty() {
        return StringUtils.isNotEmpty(pinCode) || StringUtils.isNotEmpty(city) || StringUtils.isNotEmpty(address);
    }

    public boolean isPinCodeInvalid() {
        return pinCode != null && pinCode.length() > 10;
    }

    public boolean isCityInvalid() {
        return city != null && city.length() > 300;
    }

    public boolean isAddressInvalid() {
        return address != null && address.length() > 300;
    }
}
