package org.egov.user.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration representing address classifications in the system.
 *
 * <p>In alignment with UPYOG platform architecture standards, valid business address classifications
 * are governed dynamically by MDMS (Master Data Management Service, module 'common-masters', master 'AddressType')
 * with fallback to the configured allowed types in {@code application.properties} ({@code address.types.allowed}).</p>
 *
 * <p>This enum retains core backward-compatible classifications (PERMANENT, CORRESPONDENCE, OTHER).
 * Any dynamic classifications introduced via MDMS are mapped to {@link #OTHER} at the enum layer
 * while preserving the raw master-data string in {@code Address.getAddressType()}.</p>
 */
public enum AddressType {
    PERMANENT("PERMANENT"),
    CORRESPONDENCE("CORRESPONDENCE"),
    USUALADDRESS("USUALADDRESS"),
    EVENTADDRESS("EVENTADDRESS"),
    PRESENTADDRESS("PRESENTADDRESS"),
    PROPERTYADDRESS("PROPERTYADDRESS"),
    // This category is added to handle the address type of other apart from the above-mentioned types and also this is part of V2 user api endpoints
    OTHER("OTHER");

    /**
     * Resolves an {@link AddressType} from a case-insensitive string value or alias.
     * Falls back to {@link #OTHER} if the type is dynamic (e.g. configured in MDMS).
     *
     * @param text input address type string
     * @return matching {@link AddressType}, or OTHER if input is a dynamic MDMS type
     */
    @JsonCreator
    public static AddressType fromValue(String text) {
        if (text == null) {
            return null;
        }
        String clean = text.trim().toUpperCase();
        if ("CORRESPONDANCE".equals(clean)) {
            return CORRESPONDENCE;
        }
        for (AddressType b : AddressType.values()) {
            if (b.name().equalsIgnoreCase(clean) || String.valueOf(b.value).equalsIgnoreCase(clean)) {
                return b;
            }
        }
        return OTHER;
    }

    private String value;

    AddressType(String value) {
        this.value = value;
    }

    /**
     * Returns the string representation of the enum value.
     *
     * @return address type string value
     */
    public String getValue() {
        return value;
    }

    /**
     * String representation matching enum name.
     *
     * @return enum name
     */
    @Override
    public String toString() {
        return name();
    }

}
