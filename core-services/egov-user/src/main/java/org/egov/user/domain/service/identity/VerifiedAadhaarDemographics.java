package org.egov.user.domain.service.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.user.domain.model.enums.Gender;

import java.util.Date;

/**
 * Domain DTO representing verified e-KYC demographic attributes returned by an identity verification provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifiedAadhaarDemographics {

    private String maskedAadhaar;
    private String name;
    private Gender gender;
    private Date dob;
    private String mobileNumber;
    private String photo;
    private boolean verified;
}
