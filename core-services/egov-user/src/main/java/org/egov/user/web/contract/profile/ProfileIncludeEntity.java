package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;

/**
 * Enumeration of modular citizen entity sections that can be requested in profile search ({@code /profile/_search}).
 * <p>
 * Controls selective entity enrichment to prevent unnecessary database queries and PII exposure:
 * <ul>
 *   <li>{@link #PROFILE} - Citizen core demographic, contact, and audit attributes</li>
 *   <li>{@link #ADDRESS} - Active postal address records</li>
 *   <li>{@link #DOCUMENT} - Uploaded KYC and identity proof documents</li>
 *   <li>{@link #PROPERTY} - Declared rural and urban property assets</li>
 *   <li>{@link #VEHICLE} - Registered motor vehicle assets</li>
 *   <li>{@link #INCOME} - Declared annual income and fiscal identifiers</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public enum ProfileIncludeEntity {
    /**
     * Includes citizen personal demographic attributes, contact info, and roles.
     */
    PROFILE,

    /**
     * Includes active citizen address records.
     */
    ADDRESS,

    /**
     * Includes uploaded citizen KYC documents.
     */
    DOCUMENT,

    /**
     * Includes declared citizen property assets (rural and urban).
     */
    PROPERTY,

    /**
     * Includes registered citizen motor vehicle assets.
     */
    VEHICLE,

    /**
     * Includes declared citizen annual income and fiscal identification data.
     */
    INCOME;

    /**
     * Parses a case-insensitive string into its corresponding {@link ProfileIncludeEntity} constant.
     * Validates input and throws an HTTP 400 {@link CustomException} if the string does not match any valid enum.
     *
     * @param val string representation of the include section (e.g. "PROFILE", "ADDRESS", "DOCUMENT", "PROPERTY", "VEHICLE", "INCOME")
     * @return matching {@link ProfileIncludeEntity} constant, or null if input is blank
     * @throws CustomException if the provided value is unknown or invalid (HTTP 400)
     */
    @JsonCreator
    public static ProfileIncludeEntity fromString(String val) {
        if (StringUtils.isBlank(val)) {
            return null;
        }
        for (ProfileIncludeEntity e : values()) {
            if (e.name().equalsIgnoreCase(val.trim())) {
                return e;
            }
        }
        throw new CustomException("INVALID_INCLUDE_ENTITY",
                "Invalid profile include entity: '" + val + "'. Allowed values are: PROFILE, ADDRESS, DOCUMENT, PROPERTY, VEHICLE, INCOME");
    }

    /**
     * Returns the uppercase string name of this include entity.
     *
     * @return name of the enum constant
     */
    @JsonValue
    public String toValue() {
        return name();
    }
}
