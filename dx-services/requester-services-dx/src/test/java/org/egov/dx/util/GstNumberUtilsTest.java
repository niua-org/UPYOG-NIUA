package org.egov.dx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GstNumberUtilsTest {

    /**
     * Tests Normalize Gstin behavior.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testNormalizeGstin() {
        assertEquals("03AAAPS0001B1ZW", GstNumberUtils.normalizeGstin("  03aaaps0001b1zw "));
        assertNull(GstNumberUtils.normalizeGstin("  "));
        assertNull(GstNumberUtils.normalizeGstin(null));
    }

    /**
     * Tests Is Valid Gstin under the valid formats and checksum scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsValidGstin_ValidFormatsAndChecksum() {
        // Valid format + valid Mod-36 check digits
        assertTrue(GstNumberUtils.isValidGstin("27AAPFU0939F1ZV"));
        assertTrue(GstNumberUtils.isValidGstin("03AAAPS0001B1ZW"));
        assertTrue(GstNumberUtils.isValidGstin("03CANCE0001L1Z1"));
        assertTrue(GstNumberUtils.isValidGstin("27aapfu0939f1zv")); // case normalization
    }

    /**
     * Tests Is Valid Gstin under the invalid formats scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsValidGstin_InvalidFormats() {
        assertFalse(GstNumberUtils.isValidGstin(null));
        assertFalse(GstNumberUtils.isValidGstin(""));
        assertFalse(GstNumberUtils.isValidGstin("03AAAPS0001B1Z")); // 14 chars
        assertFalse(GstNumberUtils.isValidGstin("03AAAPS0001B1ZW5")); // 16 chars
        assertFalse(GstNumberUtils.isValidGstin("AAAPS0001B1ZW03")); // wrong pattern
        assertFalse(GstNumberUtils.isValidGstin("03AAAPS0001B1AW")); // 'A' instead of 'Z' in 14th pos
    }

    /**
     * Tests Check Digit Calculation And Validation behavior.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testCheckDigitCalculationAndValidation() {
        // Canonical GST Council example
        assertEquals('V', GstNumberUtils.calculateCheckDigit("27AAPFU0939F1Z"));
        assertTrue(GstNumberUtils.validateCheckDigit("27AAPFU0939F1ZV"));
        assertFalse(GstNumberUtils.validateCheckDigit("27AAPFU0939F1Z5"));

        // Mod-36 check digit for 03AAAPS0001B1Z
        assertEquals('W', GstNumberUtils.calculateCheckDigit("03AAAPS0001B1Z"));
        assertTrue(GstNumberUtils.validateCheckDigit("03AAAPS0001B1ZW"));
        assertFalse(GstNumberUtils.validateCheckDigit("03AAAPS0001B1Z5"));

        // Format-only check without checksum validation
        assertTrue(GstNumberUtils.isValidGstin("03AAAPS0001B1Z5", false));
        assertFalse(GstNumberUtils.isValidGstin("03AAAPS0001B1Z5", true));
    }

    /**
     * Tests Mask Gstin behavior.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testMaskGstin() {
        assertEquals("03AAAP****1ZW", GstNumberUtils.maskGstin("03AAAPS0001B1ZW"));
        assertEquals("27AAPF****1ZV", GstNumberUtils.maskGstin("27aapfu0939f1zv"));
        assertEquals("XX****XX", GstNumberUtils.maskGstin(null));
        assertEquals("XX****XX", GstNumberUtils.maskGstin("   "));
        assertEquals("******", GstNumberUtils.maskGstin("03A"));
        assertEquals("03****Z5", GstNumberUtils.maskGstin("03XYZ5"));
    }

    /**
     * Tests Extract Pan From Gstin behavior.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testExtractPanFromGstin() {
        assertEquals("AAAPS0001B", GstNumberUtils.extractPanFromGstin("03AAAPS0001B1ZW"));
        assertEquals("AAPFU0939F", GstNumberUtils.extractPanFromGstin("27AAPFU0939F1ZV"));
        assertNull(GstNumberUtils.extractPanFromGstin(null));
        assertNull(GstNumberUtils.extractPanFromGstin("03"));
    }

    /**
     * Tests Extract State Code behavior.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testExtractStateCode() {
        assertEquals("03", GstNumberUtils.extractStateCode("03AAAPS0001B1ZW"));
        assertEquals("27", GstNumberUtils.extractStateCode("27AAPFU0939F1ZV"));
        assertNull(GstNumberUtils.extractStateCode(null));
        assertNull(GstNumberUtils.extractStateCode("0"));
    }

    /**
     * Tests Resolve State Name behavior.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testResolveStateName() {
        assertEquals("Punjab", GstNumberUtils.resolveStateName("03"));
        assertEquals("Chandigarh", GstNumberUtils.resolveStateName("04"));
        assertEquals("Haryana", GstNumberUtils.resolveStateName("06"));
        assertEquals("Delhi", GstNumberUtils.resolveStateName("07"));
        assertEquals("Karnataka", GstNumberUtils.resolveStateName("29"));
        assertEquals("Maharashtra", GstNumberUtils.resolveStateName("27"));
        assertEquals("Unknown State", GstNumberUtils.resolveStateName("99"));
        assertEquals("Unknown State", GstNumberUtils.resolveStateName(null));
    }
}
