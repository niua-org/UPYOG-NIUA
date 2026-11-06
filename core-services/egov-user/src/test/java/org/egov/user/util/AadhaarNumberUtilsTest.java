package org.egov.user.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AadhaarNumberUtilsTest {

    @Test
    @DisplayName("normalizeToTwelveDigits accepts raw 12-digit input")
    void normalizeToTwelveDigits_Raw_ReturnsSame() {
        assertEquals("548796521458", AadhaarNumberUtils.normalizeToTwelveDigits("548796521458"));
    }

    @Test
    @DisplayName("sameTwelveDigits compares full numbers only")
    void sameTwelveDigits_FullNumbers_Match() {
        assertTrue(AadhaarNumberUtils.sameTwelveDigits("548796521458", "548796521458"));
        assertFalse(AadhaarNumberUtils.sameTwelveDigits("548796521458", "548796521459"));
    }

    @Test
    @DisplayName("normalizeToTwelveDigits returns null for masked-only legacy values")
    void normalizeToTwelveDigits_MaskedOnly_ReturnsNull() {
        assertNull(AadhaarNumberUtils.normalizeToTwelveDigits("XXXXXXXX1458"));
    }
}
