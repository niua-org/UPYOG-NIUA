package org.upyog.family.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration representing the lifecycle and workflow application states of a Family Unit in UPYOG
 * (ACTIVE, INACTIVE, SPLIT, MERGED, PENDING_FOR_APPROVAL, APPROVED, REJECTED).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public enum FamilyStatus {
    ACTIVE,
    INACTIVE,
    SPLIT,
    MERGED,
    PENDING_FOR_APPROVAL,
    APPROVED,
    REJECTED;

    /**
     * Returns the serialized string representation of the FamilyStatus enum in UPYOG.
     *
     * @return enum constant name as string representation
     */
    @JsonValue
    public String getValue() {
        return this.name();
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
            if (StringUtils.equalsIgnoreCase(b.name(), text)) {
                return b;
            }
        }
        return null;
    }
}
