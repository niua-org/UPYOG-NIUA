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

    /**
     * Full 12-digit Aadhaar from verified e-KYC when available.
     * TODO: mask Aadhaar and move plain-text storage to Aadhaar Data Vault when integrating with UIDAI.
     */
    private String fullAadhaarNumber;

    private String maskedAadhaar;
    private String name;
    private Gender gender;
    private Date dob;
    private String mobileNumber;
    private String photo;
    private boolean verified;
}
