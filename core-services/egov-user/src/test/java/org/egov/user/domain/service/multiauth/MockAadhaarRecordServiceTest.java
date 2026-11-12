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
}
