package org.egov.user.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration test suite for verifying behavior of MaskUtilsTest.
 */
@DisplayName("MaskUtils Unit Tests")
class MaskUtilsTest {

    /**
     * Tests the functionality of test mask mobile.
     */
    @Test
    @DisplayName("Should mask mobile numbers revealing first 2 and last 2 digits")
    void testMaskMobile() {
        assertEquals("98******10", MaskUtils.maskMobile("9876543210"));
        assertEquals("98******10", MaskUtils.maskMobile("  9876543210  "));
        assertEquals("******", MaskUtils.maskMobile("12345"));
        assertNull(MaskUtils.maskMobile(null));
        assertNull(MaskUtils.maskMobile(""));
        assertNull(MaskUtils.maskMobile("   "));

        // Overload with fallback
        assertEquals("N/A", MaskUtils.maskMobile(null, "N/A"));
        assertEquals("N/A", MaskUtils.maskMobile("", "N/A"));
    }

    /**
     * Tests the functionality of test mask mobile last.
     */
    @Test
    @DisplayName("Should mask mobile numbers revealing last 4 digits for OTP hint")
    void testMaskMobileLast4() {
        assertEquals("******3210", MaskUtils.maskMobileLast4("9876543210"));
        assertEquals("******3210", MaskUtils.maskMobileLast4("  9876543210  "));
        assertEquals("******12", MaskUtils.maskMobileLast4("12"));
        assertEquals("******", MaskUtils.maskMobileLast4(null));
        assertEquals("******", MaskUtils.maskMobileLast4(""));
    }

    /**
     * Tests the functionality of test mask aadhaar.
     */
    @Test
    @DisplayName("Should mask 12-digit Aadhaar numbers with XXXXXXXX prefix")
    void testMaskAadhaar() {
        assertEquals("XXXXXXXX9012", MaskUtils.maskAadhaar("123456789012"));
        assertEquals("XXXXXXXX9012", MaskUtils.maskAadhaar("1234 5678 9012"));
        assertEquals("XXXXXXXX9012", MaskUtils.maskAadhaar("XXXXXXXX9012"));
        assertEquals("XXXXXXXX****", MaskUtils.maskAadhaar("12"));
        assertNull(MaskUtils.maskAadhaar(null));
        assertNull(MaskUtils.maskAadhaar(""));
        assertEquals("N/A", MaskUtils.maskAadhaar(null, "N/A"));
    }

    /**
     * Tests the functionality of test mask identifier.
     */
    @Test
    @DisplayName("Should mask identifiers preserving last 4 characters")
    void testMaskIdentifier() {
        assertEquals("******8662", MaskUtils.maskIdentifier("REF-12348662"));
        assertEquals("******", MaskUtils.maskIdentifier("123"));
        assertEquals("******", MaskUtils.maskIdentifier(null));
        assertEquals("******", MaskUtils.maskIdentifier(""));
    }

    /**
     * Tests the functionality of test mask token.
     */
    @Test
    @DisplayName("Should mask tokens revealing first 8 and last 4 characters")
    void testMaskToken() {
        assertEquals("585d8869...e040", MaskUtils.maskToken("585d8869b2084c8a81615f02f928e040"));
        assertEquals("******", MaskUtils.maskToken("short"));
        assertEquals("******", MaskUtils.maskToken(null));
        assertEquals("******", MaskUtils.maskToken(""));
    }

    /**
     * Tests the functionality of test mask email.
     */
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
