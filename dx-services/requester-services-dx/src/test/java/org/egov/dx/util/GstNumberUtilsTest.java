package org.egov.dx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GstNumberUtilsTest {

    @Test
    void testNormalizeGstin() {
        assertEquals("03AAAPS0001B1Z5", GstNumberUtils.normalizeGstin("  03aaaps0001b1z5 "));
        assertNull(GstNumberUtils.normalizeGstin("  "));
        assertNull(GstNumberUtils.normalizeGstin(null));
    }

    @Test
    void testIsValidGstin_ValidFormats() {
        assertTrue(GstNumberUtils.isValidGstin("03AAAPS0001B1Z5"));
        assertTrue(GstNumberUtils.isValidGstin("29INFRC1234B1Z2"));
        assertTrue(GstNumberUtils.isValidGstin("07mohug5678c1z8")); // normalized to uppercase
        assertTrue(GstNumberUtils.isValidGstin("03PTLAM0012A1Z5"));
    }

    @Test
    void testIsValidGstin_InvalidFormats() {
        assertFalse(GstNumberUtils.isValidGstin(null));
        assertFalse(GstNumberUtils.isValidGstin(""));
        assertFalse(GstNumberUtils.isValidGstin("03AAAPS0001B1Z")); // 14 chars
        assertFalse(GstNumberUtils.isValidGstin("03AAAPS0001B1Z55")); // 16 chars
        assertFalse(GstNumberUtils.isValidGstin("AAAPS0001B1Z503")); // wrong pattern
        assertFalse(GstNumberUtils.isValidGstin("03AAAPS0001B1A5")); // 'A' instead of 'Z' in 14th pos
    }

    @Test
    void testMaskGstin() {
        assertEquals("03AAAP****1Z5", GstNumberUtils.maskGstin("03AAAPS0001B1Z5"));
        assertEquals("29INFR****1Z2", GstNumberUtils.maskGstin("29infrc1234b1z2"));
        assertEquals("XX****XX", GstNumberUtils.maskGstin(null));
        assertEquals("XX****XX", GstNumberUtils.maskGstin("   "));
        assertEquals("******", GstNumberUtils.maskGstin("03A"));
        assertEquals("03****Z5", GstNumberUtils.maskGstin("03XYZ5"));
    }

    @Test
    void testExtractPanFromGstin() {
        assertEquals("AAAPS0001B", GstNumberUtils.extractPanFromGstin("03AAAPS0001B1Z5"));
        assertEquals("INFRC1234B", GstNumberUtils.extractPanFromGstin("29INFRC1234B1Z2"));
        assertNull(GstNumberUtils.extractPanFromGstin(null));
        assertNull(GstNumberUtils.extractPanFromGstin("03"));
    }

    @Test
    void testExtractStateCode() {
        assertEquals("03", GstNumberUtils.extractStateCode("03AAAPS0001B1Z5"));
        assertEquals("29", GstNumberUtils.extractStateCode("29INFRC1234B1Z2"));
        assertNull(GstNumberUtils.extractStateCode(null));
        assertNull(GstNumberUtils.extractStateCode("0"));
    }

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
