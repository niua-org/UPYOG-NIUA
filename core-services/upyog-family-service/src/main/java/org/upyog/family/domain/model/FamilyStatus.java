package org.upyog.family.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration representing the lifecycle state of a Family Unit in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
import org.upyog.family.util.FamilyConstants;

public enum FamilyStatus {
    ACTIVE(FamilyConstants.STATUS_ACTIVE),
    INACTIVE(FamilyConstants.STATUS_INACTIVE),
    SPLIT(FamilyConstants.STATUS_SPLIT),
    MERGED(FamilyConstants.STATUS_MERGED);

    private final String value;

    FamilyStatus(String value) {
        this.value = value;
    }

    /**
     * Returns the serialized string representation of the FamilyStatus enum in UPYOG.
     *
     * @return enum string representation
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Deserializes a case-insensitive string into its corresponding FamilyStatus enum value in UPYOG.
     *
     * @param text string value to parse
     * @return matching FamilyStatus instance, or null if unparseable
     */
    @JsonCreator
    public static FamilyStatus fromValue(String text) {
        for (FamilyStatus b : FamilyStatus.values()) {
            if (StringUtils.equalsIgnoreCase(b.value, text)) {
                return b;
            }
        }
        return null;
    }
}
