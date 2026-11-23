package org.egov.dx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PanNumberUtilsTest {

    /**
     * Tests Normalize Pan behavior.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testNormalizePan() {
        assertEquals("ABCDE1234F", PanNumberUtils.normalizePan("  abcde1234f "));
        assertNull(PanNumberUtils.normalizePan("  "));
        assertNull(PanNumberUtils.normalizePan(null));
    }

    /**
     * Tests Is Valid Pan under the valid formats scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsValidPan_ValidFormats() {
        assertTrue(PanNumberUtils.isValidPan("ABCDE1234F"));
        assertTrue(PanNumberUtils.isValidPan("BLRPA0001A"));
        assertTrue(PanNumberUtils.isValidPan("delpa9876a")); // lower case gets normalized
        assertTrue(PanNumberUtils.isValidPan("CHDGB5432B"));
    }

    /**
     * Tests Is Valid Pan under the invalid formats scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsValidPan_InvalidFormats() {
        assertFalse(PanNumberUtils.isValidPan(null));
        assertFalse(PanNumberUtils.isValidPan(""));
        assertFalse(PanNumberUtils.isValidPan("ABCDE1234")); // 9 chars
        assertFalse(PanNumberUtils.isValidPan("ABCDE1234FF")); // 11 chars
        assertFalse(PanNumberUtils.isValidPan("12345ABCDE")); // wrong pattern
        assertFalse(PanNumberUtils.isValidPan("ABCD11234F")); // 4 letters instead of 5
        assertFalse(PanNumberUtils.isValidPan("ABCDEA234F")); // letter in digits section
    }

    /**
     * Tests Mask Pan behavior.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testMaskPan() {
        assertEquals("ABCDE****F", PanNumberUtils.maskPan("ABCDE1234F"));
        assertEquals("BLRPA****A", PanNumberUtils.maskPan("blrpa0001a"));
        assertEquals("XXXX****XX", PanNumberUtils.maskPan(null));
        assertEquals("XXXX****XX", PanNumberUtils.maskPan("   "));
        assertEquals("******", PanNumberUtils.maskPan("ABC"));
        assertEquals("AB****YZ", PanNumberUtils.maskPan("ABCXYZ"));
    }

    /**
     * Tests Resolve Pan Category behavior.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testResolvePanCategory() {
        assertEquals("Individual", PanNumberUtils.resolvePanCategory("ABCPE1234F"));
        assertEquals("Company", PanNumberUtils.resolvePanCategory("ABCCE1234F"));
        assertEquals("Hindu Undivided Family", PanNumberUtils.resolvePanCategory("ABCHE1234F"));
        assertEquals("Firm", PanNumberUtils.resolvePanCategory("ABCFE1234F"));
        assertEquals("Association of Persons (AOP)", PanNumberUtils.resolvePanCategory("ABCAE1234F"));
        assertEquals("Trust", PanNumberUtils.resolvePanCategory("ABCTE1234F"));
        assertEquals("Body of Individuals (BOI)", PanNumberUtils.resolvePanCategory("ABCBE1234F"));
        assertEquals("Local Authority", PanNumberUtils.resolvePanCategory("ABCLE1234F"));
        assertEquals("Artificial Juridical Person", PanNumberUtils.resolvePanCategory("ABCJE1234F"));
        assertEquals("Government", PanNumberUtils.resolvePanCategory("ABCGE1234F"));
        assertEquals("Other Entity", PanNumberUtils.resolvePanCategory("ABCZE1234F"));
        assertEquals("Other", PanNumberUtils.resolvePanCategory("ABC"));
        assertEquals("Other", PanNumberUtils.resolvePanCategory(null));
    }

    /**
     * Tests Normalize Dob To Dd Mm Yyyy behavior.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testNormalizeDobToDdMmYyyy() {
        assertEquals("15/08/1990", PanNumberUtils.normalizeDobToDdMmYyyy("1990-08-15"));
        assertEquals("15/08/1990", PanNumberUtils.normalizeDobToDdMmYyyy("15/08/1990"));
        assertNull(PanNumberUtils.normalizeDobToDdMmYyyy(null));
        assertNull(PanNumberUtils.normalizeDobToDdMmYyyy("   "));
    }
}
