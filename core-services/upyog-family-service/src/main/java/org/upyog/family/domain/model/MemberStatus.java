package org.upyog.family.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration representing the membership status of an individual within a Family Unit in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
import org.upyog.family.util.FamilyConstants;

public enum MemberStatus {
    ACTIVE(FamilyConstants.STATUS_ACTIVE),
    EXITED(FamilyConstants.STATUS_EXITED),
    PENDING_APPROVAL(FamilyConstants.STATUS_PENDING_APPROVAL);

    private final String value;

    MemberStatus(String value) {
        this.value = value;
    }

    /**
     * Returns the serialized string representation of the MemberStatus enum in UPYOG.
     *
     * @return enum string representation
     */
    @JsonValue
    public String getValue() {
        return value;
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
            if (StringUtils.equalsIgnoreCase(b.value, text)) {
                return b;
            }
        }
        return null;
    }
}
