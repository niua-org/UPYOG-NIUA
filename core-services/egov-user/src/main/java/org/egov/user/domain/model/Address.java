package org.egov.user.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.service.utils.UserConstants;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing a user postal/residential address.
 * Supports legacy address fields as well as V2 structural enhancements (house numbers, landmarks, geo-coordinates).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Address {
    @JsonAlias({"pincode", "pin_code"})
    private String pinCode;
    private String city;
    private String address;
    private AddressType type;
    private Long id;
    private String tenantId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long userId;

    @JsonProperty("uuid")
    @JsonAlias({"userUuid", "user_uuid"})
    private String uuid;

    private String addressType;
    private Long LastModifiedBy;
    private Date LastModifiedDate;
//    Adding new fields in Address model for profile update as part of V2 api
    private String address2;
    @JsonAlias({"doorNo", "doorNumber", "house_number"})
    private String houseNumber;
    private String houseName;
    @JsonAlias({"street", "street_name"})
    private String streetName;
    private String landmark;
    private String locality;
    private Long version;
    private Double latitude;
    private Double longitude;
    @JsonProperty("filestoreId")
    @JsonAlias({"filestore_id", "fileStoreId", "addressProofFileStoreId", "address_proof_filestore_id", "addressProof", "addressProofDocument"})
    private String filestoreId;
    private String status;

    @JsonProperty("addressProof")
    @JsonAlias({"addressProofDocument", "document"})
    public void setAddressProof(Object proof) {
        if (proof == null) return;
        if (proof instanceof String) {
            this.filestoreId = (String) proof;
        } else if (proof instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) proof;
            if (map.containsKey("filestoreId")) {
                this.filestoreId = String.valueOf(map.get("filestoreId"));
            } else if (map.containsKey("fileStoreId")) {
                this.filestoreId = String.valueOf(map.get("fileStoreId"));
            }
        }
    }

    @JsonProperty("documents")
    public void setAddressDocuments(List<?> docs) {
        if (docs != null && !docs.isEmpty()) {
            Object first = docs.get(0);
            if (first instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) first;
                if (map.containsKey("filestoreId")) {
                    this.filestoreId = String.valueOf(map.get("filestoreId"));
                } else if (map.containsKey("fileStoreId")) {
                    this.filestoreId = String.valueOf(map.get("fileStoreId"));
                }
            } else if (first instanceof String) {
                this.filestoreId = (String) first;
            }
        }
    }

    /**
     * Checks if pinCode, city, or address length violates database constraints.
     *
     * @return true if any field exceeds allowed lengths
     */
    @JsonIgnore
    boolean isInvalid() {
        return isPinCodeInvalid()
                || isCityInvalid()
                || isAddressInvalid();
    }

    /**
     * Checks if at least one address component is populated.
     *
     * @return true if pinCode, city, or address is non-empty
     */
    @JsonIgnore
    public boolean isNotEmpty() {
        return StringUtils.isNotEmpty(pinCode)
                || StringUtils.isNotEmpty(city)
                || StringUtils.isNotEmpty(address);
    }

    /**
     * Validates if mandatory address fields are present when mandatory field validation is enabled.
     *
     * @param mandatoryFieldsEnabled whether mandatory field validation is enabled
     * @return true if validation fails, false otherwise
     */
    @JsonIgnore
    public boolean isMandatoryFieldsMissing(boolean mandatoryFieldsEnabled) {
        if (!mandatoryFieldsEnabled) {
            return false; // Skip validation if not enabled
        }
        
        // Check if any of the mandatory fields are null or empty
        return StringUtils.isEmpty(city) 
                || StringUtils.isEmpty(pinCode) 
                || StringUtils.isEmpty(address);
    }

    /**
     * Validates that all Section 8 mandatory fields are present for address creation.
     * Mandatory: City, Locality, Street Name, House Number, Landmark, Pin Code, Address Type.
     * Non-mandatory: House Name, Latitude, Longitude.
     *
     * @throws CustomException if any mandatory address field is absent
     */
    public void validateMandatoryAddressFields() {
        if (StringUtils.isBlank(this.city)) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_CITY_MANDATORY, UserConstants.MSG_CITY_MANDATORY);
        }
        if (StringUtils.isBlank(this.locality)) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_LOCALITY_MANDATORY, UserConstants.MSG_LOCALITY_MANDATORY);
        }
        if (StringUtils.isBlank(this.streetName)) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_STREET_NAME_MANDATORY, UserConstants.MSG_STREET_NAME_MANDATORY);
        }
        if (StringUtils.isBlank(this.houseNumber)) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_HOUSE_NUMBER_MANDATORY, UserConstants.MSG_HOUSE_NUMBER_MANDATORY);
        }
        if (StringUtils.isBlank(this.landmark)) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_LANDMARK_MANDATORY, UserConstants.MSG_LANDMARK_MANDATORY);
        }
        if (StringUtils.isBlank(this.pinCode)) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_PIN_CODE_MANDATORY, UserConstants.MSG_PIN_CODE_MANDATORY);
        }
        if (this.type == null && StringUtils.isBlank(this.addressType)) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_TYPE_MANDATORY, UserConstants.MSG_ADDRESS_TYPE_MANDATORY);
        }
    }

    /**
     * Enforces Section 8 non-editable field constraints during address updates.
     * Non-editable fields: City, Locality, House Number, Pin Code.
     * Editable fields: Street Name, House Name, Landmark, Latitude, Longitude, Address Type.
     *
     * @param existingAddress the decrypted existing active address from database
     * @throws CustomException if any non-editable address field has been modified
     */
    public void validateNonEditableAddressFields(Address existingAddress) {
        if (existingAddress == null) {
            return;
        }

        // 1. City (Non-Editable)
        if (isAddressFieldModified(this.city, existingAddress.getCity())) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_NON_EDITABLE_CITY, UserConstants.MSG_NON_EDITABLE_CITY);
        }

        // 2. Locality (Non-Editable)
        if (isAddressFieldModified(this.locality, existingAddress.getLocality())) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_NON_EDITABLE_LOCALITY, UserConstants.MSG_NON_EDITABLE_LOCALITY);
        }

        // 3. House Number (Non-Editable)
        if (isAddressFieldModified(this.houseNumber, existingAddress.getHouseNumber())) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_NON_EDITABLE_HOUSE_NUMBER, UserConstants.MSG_NON_EDITABLE_HOUSE_NUMBER);
        }

        // 4. Pin Code (Non-Editable)
        if (isAddressFieldModified(this.pinCode, existingAddress.getPinCode())) {
            throw new CustomException(UserConstants.UG_USER_ADDRESS_NON_EDITABLE_PIN_CODE, UserConstants.MSG_NON_EDITABLE_PIN_CODE);
        }
    }

    private boolean isAddressFieldModified(String incoming, String existing) {
        if (StringUtils.isBlank(existing)) {
            return false;
        }
        if (StringUtils.isBlank(incoming)) {
            return false;
        }
        return !incoming.trim().equalsIgnoreCase(existing.trim());
    }
    
    /**
     * Checks if pincode length exceeds maximum permitted length (10 chars).
     *
     * @return true if pincode length is greater than 10
     */
    @JsonIgnore
    boolean isPinCodeInvalid() {
        return pinCode != null && pinCode.length() > 10;
    }
    
    /**
     * Checks if city name exceeds maximum permitted length (300 chars).
     *
     * @return true if city length is greater than 300
     */
    @JsonIgnore
    boolean isCityInvalid() {
        return city != null && city.length() > 300;
    }

    /**
     * Checks if address line exceeds maximum permitted length (300 chars).
     *
     * @return true if address length is greater than 300
     */
    @JsonIgnore
    boolean isAddressInvalid() {
        return address != null && address.length() > 300;
    }

    /**
     * Compatibility getter for {@code userUuid} mapping to {@code uuid}.
     *
     * @return the user UUID string
     */
    @JsonIgnore
    public String getUserUuid() {
        return this.uuid;
    }

    /**
     * Compatibility setter for {@code userUuid} mapping to {@code uuid}.
     *
     * @param userUuid the user UUID string
     */
    public void setUserUuid(String userUuid) {
        this.uuid = userUuid;
    }

    /**
     * Sets the {@link AddressType} enum and synchronizes string field.
     *
     * @param type address type enum
     */
    public void setType(AddressType type) {
        this.type = type;
        if (type != null) {
            this.addressType = type.name();
        }
    }

    /**
     * Sets address type by string name and synchronizes enum.
     *
     * @param addressType address type string
     */
    public void setAddressType(String addressType) {
        this.addressType = addressType;
        if (this.type == null && addressType != null) {
            this.type = AddressType.fromValue(addressType);
        }
    }

    /**
     * Returns the {@link AddressType} enum, resolving from string if needed.
     *
     * @return address type enum
     */
    public AddressType getType() {
        if (this.type == null && this.addressType != null) {
            this.type = AddressType.fromValue(this.addressType);
        }
        return this.type;
    }

    /**
     * Returns the address type string name.
     *
     * @return address type string name
     */
    public String getAddressType() {
        if (this.addressType == null && this.type != null) {
            this.addressType = this.type.name();
        }
        return this.addressType;
    }

    /**
     * Normalizes address status to lower-case.
     *
     * @param status status string
     */
    public void setStatus(String status) {
        this.status = status != null ? status.trim().toLowerCase() : null;
    }

    /**
     * Custom builder extensions for {@link Address}.
     */
    public static class AddressBuilder {
        /**
         * Sets {@code uuid} via alias {@code userUuid}.
         *
         * @param userUuid user UUID string
         * @return builder instance
         */
        public AddressBuilder userUuid(String userUuid) {
            this.uuid = userUuid;
            return this;
        }

        /**
         * Sets normalized lowercase status.
         *
         * @param status address status
         * @return builder instance
         */
        public AddressBuilder status(String status) {
            this.status = status != null ? status.trim().toLowerCase() : null;
            return this;
        }
    }
}
