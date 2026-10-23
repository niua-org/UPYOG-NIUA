package org.upyog.family.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration representing reasons for a member leaving a Family Unit in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public enum ExitReason {
    MARRIAGE("MARRIAGE"),
    DEMISE("DEMISE"),
    RELOCATION("RELOCATION"),
    OTHER("OTHER");

    private final String value;

    ExitReason(String value) {
        this.value = value;
    }

    /**
     * Returns the serialized string representation of the ExitReason enum in UPYOG.
     *
     * @return enum string representation
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Deserializes a case-insensitive string into its corresponding ExitReason enum value in UPYOG.
     *
     * @param text string value to parse
     * @return matching ExitReason instance, or null if unparseable
     */
    @JsonCreator
    public static ExitReason fromValue(String text) {
        for (ExitReason b : ExitReason.values()) {
            if (StringUtils.equalsIgnoreCase(b.value, text)) {
                return b;
            }
        }
        return null;
    }
}
