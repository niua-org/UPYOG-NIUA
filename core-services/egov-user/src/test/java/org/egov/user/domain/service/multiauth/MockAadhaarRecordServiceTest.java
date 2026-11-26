package org.egov.user.domain.service.multiauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MockAadhaarRecordServiceTest {

    @BeforeEach
    void setUp() {
        MockAadhaarRecordService.resetIndex();
    }

    @Test
    @DisplayName("Should load more than 500 mock Aadhaar records from mock/aadhaar-mock-citizens.json")
    void shouldLoadMoreThan500Records() {
        int total = MockAadhaarRecordService.getTotalRecords();
        assertTrue(total >= 500, "Expected at least 500 records, got: " + total);
        assertEquals(520, total);
    }

    @Test
    @DisplayName("Should dispense distinct new records sequentially until all records are used")
    void shouldDispenseDistinctRecordsSequentially() {
        int total = MockAadhaarRecordService.getTotalRecords();
        Set<String> dispensedNames = new HashSet<>();

        for (int i = 0; i < total; i++) {
            MockAadhaarRecordService.MockCitizenRecord record = MockAadhaarRecordService.getNextRecord();
            assertNotNull(record);
            assertNotNull(record.getName());
            assertNotNull(record.getGender());
            assertNotNull(record.getDob());
            dispensedNames.add(record.getName());
        }

        // All 520 records dispensed in one pass must be distinct
        assertEquals(total, dispensedNames.size(), "Every record in a full cycle must be distinct");

        // After all used, next call wraps around to first record
        MockAadhaarRecordService.MockCitizenRecord wrapped = MockAadhaarRecordService.getNextRecord();
        assertNotNull(wrapped);
        assertTrue(dispensedNames.contains(wrapped.getName()));
    }

    /**
     * Fix for PR Comment #16 (MockAadhaarRecordService.java:144):
     * Verifies that getRecordForAadhaar deterministically returns the exact same persona
     * for repeated calls with the same Aadhaar number.
     */
    @Test
    @DisplayName("Should deterministically return identical record for same Aadhaar number")
    void shouldDeterministicallyReturnSameRecordForSameAadhaar() {
        String testAadhaar = "999912345678";

        MockAadhaarRecordService.MockCitizenRecord firstCall = MockAadhaarRecordService.getRecordForAadhaar(testAadhaar);
        MockAadhaarRecordService.MockCitizenRecord secondCall = MockAadhaarRecordService.getRecordForAadhaar(testAadhaar);
        MockAadhaarRecordService.MockCitizenRecord thirdCall = MockAadhaarRecordService.getRecordForAadhaar(testAadhaar);

        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertNotNull(thirdCall);

        assertEquals(firstCall.getName(), secondCall.getName());
        assertEquals(firstCall.getGender(), secondCall.getGender());
        assertEquals(firstCall.getDob(), secondCall.getDob());
        assertEquals(firstCall.getCareOf(), secondCall.getCareOf());

        assertEquals(secondCall.getName(), thirdCall.getName());
    }

    /**
     * Fix for PR Comment #16 (MockAadhaarRecordService.java:144):
     * Verifies that null or blank Aadhaar gracefully delegates to sequential dispensing.
     */
    @Test
    @DisplayName("Should fall back to sequential dispensing when Aadhaar is null or blank")
    void shouldFallBackToSequentialDispensingForBlankAadhaar() {
        MockAadhaarRecordService.MockCitizenRecord nullAadhaar = MockAadhaarRecordService.getRecordForAadhaar(null);
        MockAadhaarRecordService.MockCitizenRecord blankAadhaar = MockAadhaarRecordService.getRecordForAadhaar("   ");

        assertNotNull(nullAadhaar);
        assertNotNull(blankAadhaar);
        assertNotNull(nullAadhaar.getName());
        assertNotNull(blankAadhaar.getName());
    }

    /**
     * Fix for PR Comment #17 (MockAadhaarRecordService.java:57):
     * Verifies that mock citizen records use reserved dummy non-routable mobile numbers (99999XXXXX)
     * to prevent triggering downstream SMS/OTPs to real citizens.
     */
    @Test
    @DisplayName("Should use reserved fake mobile numbers in 99999XXXXX range")
    void shouldUseReservedDummyMobileNumbers() {
        int total = MockAadhaarRecordService.getTotalRecords();
        for (int i = 0; i < total; i++) {
            MockAadhaarRecordService.MockCitizenRecord record = MockAadhaarRecordService.getNextRecord();
            assertNotNull(record.getMobileNumber(), "Mobile number must not be null for mock citizen: " + record.getName());
            assertTrue(record.getMobileNumber().matches("^99999\\d{5}$"),
                    "Mock mobile number must be in reserved non-routable test range 99999XXXXX, got: " + record.getMobileNumber());
        }
    }
}
