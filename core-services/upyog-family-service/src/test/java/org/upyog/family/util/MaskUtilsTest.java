package org.upyog.family.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MaskUtils Unit Tests")
class MaskUtilsTest {

    @Test
    @DisplayName("Should mask mobile numbers revealing first 2 and last 2 digits")
    void testMaskMobile() {
        assertEquals("79******96", MaskUtils.maskMobile("7912345696"));
        assertEquals("79******96", MaskUtils.maskMobile("  7912345696  "));
        assertEquals("******", MaskUtils.maskMobile("12345"));
        assertNull(MaskUtils.maskMobile(null));
        assertNull(MaskUtils.maskMobile(""));
        assertNull(MaskUtils.maskMobile("   "));

        // Overload with fallback
        assertEquals("N/A", MaskUtils.maskMobile(null, "N/A"));
        assertEquals("N/A", MaskUtils.maskMobile("", "N/A"));
    }

    @Test
    @DisplayName("Should mask mobile numbers revealing last 4 digits for OTP hint")
    void testMaskMobileLast4() {
        assertEquals("******3210", MaskUtils.maskMobileLast4("9876543210"));
        assertEquals("******3210", MaskUtils.maskMobileLast4("  9876543210  "));
        assertEquals("******12", MaskUtils.maskMobileLast4("12"));
        assertEquals("******", MaskUtils.maskMobileLast4(null));
        assertEquals("******", MaskUtils.maskMobileLast4(""));
    }

    @Test
    @DisplayName("Should mask 12-digit Aadhaar numbers with XXXXXXXX prefix")
    void testMaskAadhaar() {
        assertEquals("XXXXXXXX1458", MaskUtils.maskAadhaar("123456781458"));
        assertEquals("XXXXXXXX1458", MaskUtils.maskAadhaar("1234 5678 1458"));
        assertEquals("XXXXXXXX1458", MaskUtils.maskAadhaar("XXXXXXXX1458"));
        assertEquals("XXXXXXXX****", MaskUtils.maskAadhaar("12"));
        assertNull(MaskUtils.maskAadhaar(null));
        assertNull(MaskUtils.maskAadhaar(""));
        assertEquals("N/A", MaskUtils.maskAadhaar(null, "N/A"));
    }

    @Test
    @DisplayName("Should mask identifiers preserving last 4 characters")
    void testMaskIdentifier() {
        assertEquals("******8662", MaskUtils.maskIdentifier("REF-12348662"));
        assertEquals("******", MaskUtils.maskIdentifier("123"));
        assertEquals("******", MaskUtils.maskIdentifier(null));
        assertEquals("******", MaskUtils.maskIdentifier(""));
    }

    @Test
    @DisplayName("Should mask tokens revealing first 8 and last 4 characters")
    void testMaskToken() {
        assertEquals("585d8869...e040", MaskUtils.maskToken("585d8869b2084c8a81615f02f928e040"));
        assertEquals("******", MaskUtils.maskToken("short"));
        assertEquals("******", MaskUtils.maskToken(null));
        assertEquals("******", MaskUtils.maskToken(""));
    }

    @Test
    @DisplayName("Should mask email addresses")
    void testMaskEmail() {
        assertEquals("j***e@domain.com", MaskUtils.maskEmail("john.doe@domain.com"));
        assertEquals("j*@domain.com", MaskUtils.maskEmail("jo@domain.com"));
        assertEquals("******@domain.com", MaskUtils.maskEmail("j@domain.com"));
        assertNull(MaskUtils.maskEmail(null));
        assertNull(MaskUtils.maskEmail(""));
    }
}
