package org.egov.garbageservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AddressType {
    PERMANENT("PERMANENT"), CORRESPONDENCE("CORRESPONDENCE"), OTHER("OTHER");

    @JsonCreator
    public static AddressType fromValue(String text) {
        for (AddressType b : AddressType.values()) {
            if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

    private String value;

    AddressType(String value) {
        this.value = value;
    }
}
