package org.upyog.family.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * Action type tracking lineage evolution of families in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public enum LineageActionType {
    SPLIT("SPLIT"),
    MERGE("MERGE");

    private final String value;

    LineageActionType(String value) {
        this.value = value;
    }

    /**
     * Returns the serialized string representation of the LineageActionType enum in UPYOG.
     *
     * @return enum string representation
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Deserializes a case-insensitive string into its corresponding LineageActionType enum value in UPYOG.
     *
     * @param text string value to parse
     * @return matching LineageActionType instance, or null if unparseable
     */
    @JsonCreator
    public static LineageActionType fromValue(String text) {
        for (LineageActionType b : LineageActionType.values()) {
            if (StringUtils.equalsIgnoreCase(b.value, text)) {
                return b;
            }
        }
        return null;
    }
}
