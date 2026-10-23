package org.upyog.family.web.contract.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Family member contract provided by external State Registry (e.g., Haryana PPP).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateFamilyMember {

    @JsonProperty("memberId")
    private String memberId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("relationshipWithHead")
    private String relationshipWithHead;

    @JsonProperty("isHead")
    private Boolean isHead;

    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @JsonProperty("maskedMobile")
    private String maskedMobile;

    @JsonProperty("aadhaarNumber")
    private String aadhaarNumber;

    @JsonProperty("maskedAadhaar")
    private String maskedAadhaar;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("dob")
    private String dob;

    @JsonProperty("isVerified")
    @Builder.Default
    private Boolean isVerified = true;

    @JsonProperty("userUuid")
    private String userUuid;
}
