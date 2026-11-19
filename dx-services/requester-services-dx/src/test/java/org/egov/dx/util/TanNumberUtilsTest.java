package org.egov.dx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TanNumberUtilsTest {

    @Test
    void testNormalizeTan() {
        assertEquals("BLRM12345C", TanNumberUtils.normalizeTan("  blrm12345c "));
        assertNull(TanNumberUtils.normalizeTan("  "));
        assertNull(TanNumberUtils.normalizeTan(null));
    }

    @Test
    void testIsValidTan_ValidFormats() {
        assertTrue(TanNumberUtils.isValidTan("BLRM12345C"));
        assertTrue(TanNumberUtils.isValidTan("PTLA00123A"));
        assertTrue(TanNumberUtils.isValidTan("delg98765a")); // lower case gets normalized
        assertTrue(TanNumberUtils.isValidTan("CHDG54321B"));
    }

    @Test
    void testIsValidTan_InvalidFormats() {
        assertFalse(TanNumberUtils.isValidTan(null));
        assertFalse(TanNumberUtils.isValidTan(""));
        assertFalse(TanNumberUtils.isValidTan("BLRM12345")); // 9 chars
        assertFalse(TanNumberUtils.isValidTan("BLRM12345CC")); // 11 chars
        assertFalse(TanNumberUtils.isValidTan("12345BLRMC")); // wrong pattern
        assertFalse(TanNumberUtils.isValidTan("BLR112345C")); // 3 letters instead of 4
        assertFalse(TanNumberUtils.isValidTan("BLRMA2345C")); // letter in digits section
    }

    @Test
    void testMaskTan() {
        assertEquals("BLRM****5C", TanNumberUtils.maskTan("BLRM12345C"));
        assertEquals("PTLA****3A", TanNumberUtils.maskTan("ptla00123a"));
        assertEquals("XXXX****XX", TanNumberUtils.maskTan(null));
        assertEquals("XXXX****XX", TanNumberUtils.maskTan("   "));
        assertEquals("******", TanNumberUtils.maskTan("BLR"));
        assertEquals("AB****YZ", TanNumberUtils.maskTan("ABCXYZ"));
    }

    @Test
    void testResolveDeductorCategory() {
        assertEquals("Company", TanNumberUtils.resolveDeductorCategory("BLRC12345A"));
        assertEquals("Local Authority", TanNumberUtils.resolveDeductorCategory("PTLL00123B"));
        assertEquals("Central Government", TanNumberUtils.resolveDeductorCategory("DELG98765C"));
        assertEquals("State Government", TanNumberUtils.resolveDeductorCategory("CHDS54321D"));
        assertEquals("Individual / HUF", TanNumberUtils.resolveDeductorCategory("DELP11223E"));
        assertEquals("Firm", TanNumberUtils.resolveDeductorCategory("AMRF99887F"));
        assertEquals("Trust", TanNumberUtils.resolveDeductorCategory("BLRT11111G"));
        assertEquals("Autonomous Body", TanNumberUtils.resolveDeductorCategory("CHDD22222H"));
        assertEquals("Association of Persons (AOP)", TanNumberUtils.resolveDeductorCategory("MUMA33333I"));
        assertEquals("Body of Individuals (BOI)", TanNumberUtils.resolveDeductorCategory("MUMB44444J"));
        assertEquals("Artificial Juridical Person", TanNumberUtils.resolveDeductorCategory("MUMJ55555K"));
        assertEquals("Other Entity", TanNumberUtils.resolveDeductorCategory("MUMZ66666L"));
        assertEquals("Other", TanNumberUtils.resolveDeductorCategory("MUM"));
        assertEquals("Other", TanNumberUtils.resolveDeductorCategory(null));
    }
}
