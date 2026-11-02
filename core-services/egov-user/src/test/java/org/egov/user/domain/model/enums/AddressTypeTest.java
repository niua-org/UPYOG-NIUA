package org.egov.user.domain.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AddressTypeTest {

    @Test
    @DisplayName("Each AddressType constant should have a distinct value matching its name")
    public void testEachEnumHasDistinctMatchingValue() {
        for (AddressType type : AddressType.values()) {
            assertEquals(type.name(), type.getValue(), "Enum " + type.name() + " value must match its name");
            assertEquals(type.name(), type.toString(), "Enum " + type.name() + " toString must match its name");
        }
    }

    @Test
    @DisplayName("fromValue should resolve exact and case-insensitive enum names")
    public void testFromValueValidNames() {
        assertEquals(AddressType.PERMANENT, AddressType.fromValue("PERMANENT"));
        assertEquals(AddressType.PERMANENT, AddressType.fromValue("permanent"));
        assertEquals(AddressType.CORRESPONDENCE, AddressType.fromValue("CORRESPONDENCE"));
        assertEquals(AddressType.CORRESPONDENCE, AddressType.fromValue("correspondence"));
        assertEquals(AddressType.USUALADDRESS, AddressType.fromValue("USUALADDRESS"));
        assertEquals(AddressType.EVENTADDRESS, AddressType.fromValue("EVENTADDRESS"));
        assertEquals(AddressType.PRESENTADDRESS, AddressType.fromValue("PRESENTADDRESS"));
        assertEquals(AddressType.PROPERTYADDRESS, AddressType.fromValue("PROPERTYADDRESS"));
        assertEquals(AddressType.OTHER, AddressType.fromValue("OTHER"));
    }

    @Test
    @DisplayName("fromValue should fallback to OTHER for dynamic MDMS types or unknown values")
    public void testFromValueDynamicFallback() {
        assertEquals(AddressType.OTHER, AddressType.fromValue("OFFICE"));
        assertEquals(AddressType.OTHER, AddressType.fromValue("COMMERCIAL"));
        assertEquals(AddressType.OTHER, AddressType.fromValue("TEMPORARY"));
        assertEquals(AddressType.OTHER, AddressType.fromValue("SOME_NEW_MDMS_TYPE"));
    }

    @Test
    @DisplayName("fromValue should return null for null input")
    public void testFromValueNull() {
        assertNull(AddressType.fromValue(null));
    }
}
