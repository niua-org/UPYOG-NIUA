package org.upyog.family.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration representing the membership lifecycle states of an individual within a Family Unit in UPYOG
 * (ACTIVE, EXITED, PENDING_FOR_APPROVAL, JOINED).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public enum MemberStatus {
    ACTIVE,
    EXITED,
    PENDING_FOR_APPROVAL,
    JOINED;

    /**
     * Returns the serialized string representation of the MemberStatus enum in UPYOG.
     *
     * @return enum constant name as string representation
     */
    @JsonValue
    public String getValue() {
        return this.name();
    }

    /**
     * Deserializes a case-insensitive string into its corresponding MemberStatus enum value in UPYOG.
     *
     * @param text string value to parse
     * @return matching MemberStatus instance, or null if unparseable
     */
    @JsonCreator
    public static MemberStatus fromValue(String text) {
        for (MemberStatus b : MemberStatus.values()) {
            if (StringUtils.equalsIgnoreCase(b.name(), text)) {
                return b;
            }
        }
        return null;
    }
}
