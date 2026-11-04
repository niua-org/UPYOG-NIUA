package org.egov.user.web.contract.multiauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * Supported authentication types for the loginv2 multi-auth endpoint.
 *
 * <ul>
 *   <li>{@link #MOBILE_NUMBER}: Standard OTP-based login via citizen mobile number</li>
 *   <li>{@link #AADHAAR_NUMBER}: e-KYC identity verification via UIDAI Aadhaar gateway OTP</li>
 *   <li>{@link #FAMILY_ID}: Household relationship-based login via Family ID & member OTP</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public enum AuthType {
    MOBILE_NUMBER("MOBILE_NUMBER"),
    AADHAAR_NUMBER("AADHAAR_NUMBER"),
    FAMILY_ID("FAMILY_ID");

    private final String value;

    AuthType(String value) {
        this.value = value;
    }

    /**
     * Retrieves the serialized string representation of the auth type.
     *
     * @return the uppercase enum string value
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Parses a string input into a matching {@link AuthType}, handling common aliases.
     *
     * @param text input authentication type string
     * @return resolved {@link AuthType}, or {@code null} if blank
     * @throws IllegalArgumentException if text doesn't match any supported type
     */
    @JsonCreator
    public static AuthType fromValue(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        for (AuthType b : AuthType.values()) {
            if (b.value.equalsIgnoreCase(text.trim()) || b.name().equalsIgnoreCase(text.trim())) {
                return b;
            }
        }
        if (text.equalsIgnoreCase("family") || text.equalsIgnoreCase("familyId") || text.equalsIgnoreCase("state_family") || text.equalsIgnoreCase("stateFamily")) {
            return FAMILY_ID;
        }
        if (text.equalsIgnoreCase("phone") || text.equalsIgnoreCase("mobile") || text.equalsIgnoreCase("mobileNumber")) {
            return MOBILE_NUMBER;
        }
        if (text.equalsIgnoreCase("aadhaar") || text.equalsIgnoreCase("aadhar")
                || text.equalsIgnoreCase("aadhaarNumber") || text.equalsIgnoreCase("aadharNumber")
                || text.equalsIgnoreCase("aadhar_number")) {
            return AADHAAR_NUMBER;
        }
        throw new IllegalArgumentException("Unsupported AuthType: " + text + ". Expected MOBILE_NUMBER, AADHAAR_NUMBER, or FAMILY_ID.");
    }

    /**
     * String representation of the enum value.
     *
     * @return string value
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
