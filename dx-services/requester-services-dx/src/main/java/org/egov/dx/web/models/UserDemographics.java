package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Model representing demographic and address information of a citizen
 * extracted from authenticated UIDAI Aadhaar e-KYC response.
 *
 * <p>This model is returned to upstream services (such as egov-user) upon successful OTP verification
 * so that citizen profile and address records can be pre-filled or synchronized.</p>
 *
 * <p>The citizen photo is excluded from {@link #toString()} to prevent massive string output in application logs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"photo"})
public class UserDemographics {

    /**
     * Full legal name of the citizen as recorded in Aadhaar.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Date of birth string (e.g. "YYYY-MM-DD" or "DD-MM-YYYY").
     */
    @JsonProperty("dob")
    private String dob;

    /**
     * Gender code ('M', 'F', 'T').
     */
    @JsonProperty("gender")
    private String gender;

    /**
     * Care-of / Guardian / Relationship information (e.g., "S/O: Parent Name").
     */
    @JsonProperty("careOf")
    private String careOf;

    /**
     * House / Flat / Door number.
     */
    @JsonProperty("doorNo")
    private String doorNo;

    /**
     * Street / Road name.
     */
    @JsonProperty("street")
    private String street;

    /**
     * Nearby landmark.
     */
    @JsonProperty("landmark")
    private String landmark;

    /**
     * Locality / Mohalla / Sub-district.
     */
    @JsonProperty("locality")
    private String locality;

    /**
     * City / Town / Village (VTC).
     */
    @JsonProperty("city")
    private String city;

    /**
     * District name.
     */
    @JsonProperty("district")
    private String district;

    /**
     * State / Union Territory name.
     */
    @JsonProperty("state")
    private String state;

    /**
     * 6-digit postal PIN code.
     */
    @JsonProperty("pincode")
    private String pincode;

    /**
     * Country name (e.g. "India").
     */
    @JsonProperty("country")
    private String country;

    /**
     * Full concatenated address string from UIDAI.
     */
    @JsonProperty("fullAddress")
    private String fullAddress;

    /**
     * Base64 encoded photograph string retrieved from UIDAI e-KYC.
     */
    @JsonProperty("photo")
    private String photo;
}
