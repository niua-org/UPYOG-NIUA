package org.egov.dx.adapter.vendor.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Inbound DTO representing the successful response received from the third-party Aadhaar KYC gateway
 * upon valid OTP verification.
 *
 * <p>Contains verified e-KYC demographic data (name, DOB, gender, care-of, full address, and structured address fields).
 * The user photo (base64 string) is excluded from {@link #toString()} to prevent log bloat.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class VendorVerifyOtpSuccessResponse {

    /**
     * HTTP response code returned by the vendor gateway (e.g. 200).
     */
    @JsonProperty("code")
    private Integer code;

    /**
     * Epoch timestamp of the response generation.
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * Unique vendor transaction ID associated with this verification.
     */
    @JsonProperty("transaction_id")
    @JsonAlias({"transactionId", "txnId"})
    private String transactionId;

    /**
     * Detailed demographic payload returned upon successful verification.
     */
    @JsonProperty("data")
    private VendorVerifyOtpSuccessResponseDetails data;

    /**
     * Nested class containing Aadhaar demographic and KYC details.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString(exclude = {"photo"})
    public static class VendorVerifyOtpSuccessResponseDetails {

        /**
         * Gateway entity descriptor.
         */
        @JsonProperty("@entity")
        @JsonAlias("entity")
        private String entity;

        /**
         * Reference ID correlating this verification to the OTP session.
         */
        @JsonProperty("reference_id")
        @JsonAlias("referenceId")
        private String referenceId;

        /**
         * Masked Aadhaar number (e.g. "XXXXXXXX1234").
         */
        @JsonProperty("aadhaar_number")
        @JsonAlias("aadhaarNumber")
        private String aadhaarNumber;

        /**
         * Citizen's full name as recorded in UIDAI.
         */
        @JsonProperty("name")
        private String name;

        /**
         * Date of birth formatted string (e.g. "YYYY-MM-DD" or "DD-MM-YYYY").
         */
        @JsonProperty("dob")
        private String dob;

        /**
         * Gender indicator ('M', 'F', 'T').
         */
        @JsonProperty("gender")
        private String gender;

        /**
         * Verification status (e.g., "VALID", "SUCCESS").
         */
        @JsonProperty("status")
        private String status;

        /**
         * Gateway response message (e.g., "Aadhaar verified successfully").
         */
        @JsonProperty("message")
        private String message;

        /**
         * Care-of / Guardian details (e.g., "S/O: John Doe", "W/O: ...").
         */
        @JsonProperty("care_of")
        @JsonAlias("careOf")
        private String careOf;

        /**
         * Complete concatenated address string.
         */
        @JsonProperty("full_address")
        @JsonAlias("fullAddress")
        private String fullAddress;

        /**
         * Date of birth alternative format provided by gateway.
         */
        @JsonProperty("date_of_birth")
        @JsonAlias("dateOfBirth")
        private String dateOfBirth;

        /**
         * SHA-256 hash of the citizen's registered email address (if available in UIDAI).
         */
        @JsonProperty("email_hash")
        @JsonAlias("emailHash")
        private String emailHash;

        /**
         * Year of birth string (e.g. "1990").
         */
        @JsonProperty("year_of_birth")
        @JsonAlias("yearOfBirth")
        private String yearOfBirth;

        /**
         * SHA-256 hash of the citizen's registered mobile number.
         */
        @JsonProperty("mobile_hash")
        @JsonAlias("mobileHash")
        private String mobileHash;

        /**
         * Base64 encoded JPEG/PNG photograph of the citizen retrieved from UIDAI.
         */
        @JsonProperty("photo")
        private String photo;

        /**
         * Security share code generated for offline e-KYC verification.
         */
        @JsonProperty("share_code")
        @JsonAlias("shareCode")
        private String shareCode;

        /**
         * Structured address breakdown.
         */
        @JsonProperty("address")
        private AddressDetails address;

        /**
         * Nested breakdown of residential address fields.
         */
        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @ToString
        public static class AddressDetails {

            /**
             * Country name (e.g., "India").
             */
            @JsonProperty("country")
            private String country;

            /**
             * District name.
             */
            @JsonProperty("district")
            private String district;

            /**
             * House / Flat / Door number.
             */
            @JsonProperty("house")
            private String house;

            /**
             * Nearby landmark.
             */
            @JsonProperty("landmark")
            private String landmark;

            /**
             * 6-digit postal PIN code.
             */
            @JsonProperty("pincode")
            private String pincode;

            /**
             * Post office name.
             */
            @JsonProperty("post_office")
            @JsonAlias("postOffice")
            private String postOffice;

            /**
             * State / Union Territory name.
             */
            @JsonProperty("state")
            private String state;

            /**
             * Street / Road name.
             */
            @JsonProperty("street")
            private String street;

            /**
             * Sub-district / Tehsil / Taluk name.
             */
            @JsonProperty("subdistrict")
            private String subdistrict;

            /**
             * Village / Town / City (VTC) name.
             */
            @JsonProperty("vtc")
            private String vtc;
        }
    }
}