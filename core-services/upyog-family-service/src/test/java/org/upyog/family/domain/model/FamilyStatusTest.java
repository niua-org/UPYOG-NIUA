package org.upyog.family.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link FamilyStatus} enumeration and its deserialization logic.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
class FamilyStatusTest {

    @Test
    @DisplayName("getValue should return uppercase serialized status string")
    void getValue_ReturnsExpectedString() {
        assertEquals("ACTIVE", FamilyStatus.ACTIVE.getValue());
        assertEquals("INACTIVE", FamilyStatus.INACTIVE.getValue());
        assertEquals("SPLIT", FamilyStatus.SPLIT.getValue());
        assertEquals("MERGED", FamilyStatus.MERGED.getValue());
        assertEquals("PENDING_FOR_APPROVAL", FamilyStatus.PENDING_FOR_APPROVAL.getValue());
        assertEquals("APPROVED", FamilyStatus.APPROVED.getValue());
        assertEquals("REJECTED", FamilyStatus.REJECTED.getValue());
    }

    @Test
    @DisplayName("fromValue should parse case-insensitively for valid statuses")
    void fromValue_ValidStrings_ParsesCorrectly() {
        assertEquals(FamilyStatus.ACTIVE, FamilyStatus.fromValue("active"));
        assertEquals(FamilyStatus.ACTIVE, FamilyStatus.fromValue("ACTIVE"));
        assertEquals(FamilyStatus.INACTIVE, FamilyStatus.fromValue("inactive"));
        assertEquals(FamilyStatus.SPLIT, FamilyStatus.fromValue("SPLIT"));
        assertEquals(FamilyStatus.MERGED, FamilyStatus.fromValue("merged"));
        assertEquals(FamilyStatus.PENDING_FOR_APPROVAL, FamilyStatus.fromValue("PENDING_FOR_APPROVAL"));
        assertEquals(FamilyStatus.PENDING_FOR_APPROVAL, FamilyStatus.fromValue("pending_for_approval"));
        assertEquals(FamilyStatus.APPROVED, FamilyStatus.fromValue("approved"));
        assertEquals(FamilyStatus.REJECTED, FamilyStatus.fromValue("rejected"));
    }

    @Test
    @DisplayName("fromValue should return null for invalid or null strings")
    void fromValue_InvalidString_ReturnsNull() {
        assertNull(FamilyStatus.fromValue("UNKNOWN_STATUS"));
        assertNull(FamilyStatus.fromValue("PENDING_APPROVAL"));
        assertNull(FamilyStatus.fromValue(null));
        assertNull(FamilyStatus.fromValue(""));
    }
}
