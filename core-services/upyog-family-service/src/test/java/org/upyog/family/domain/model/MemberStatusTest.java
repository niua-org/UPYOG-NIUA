package org.upyog.family.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link MemberStatus} enumeration and its deserialization logic.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
class MemberStatusTest {

    @Test
    @DisplayName("getValue should return uppercase serialized status string")
    void getValue_ReturnsExpectedString() {
        assertEquals("ACTIVE", MemberStatus.ACTIVE.getValue());
        assertEquals("EXITED", MemberStatus.EXITED.getValue());
        assertEquals("PENDING_FOR_APPROVAL", MemberStatus.PENDING_FOR_APPROVAL.getValue());
        assertEquals("JOINED", MemberStatus.JOINED.getValue());
    }

    @Test
    @DisplayName("fromValue should parse case-insensitively for valid statuses")
    void fromValue_ValidStrings_ParsesCorrectly() {
        assertEquals(MemberStatus.ACTIVE, MemberStatus.fromValue("active"));
        assertEquals(MemberStatus.ACTIVE, MemberStatus.fromValue("ACTIVE"));
        assertEquals(MemberStatus.EXITED, MemberStatus.fromValue("exited"));
        assertEquals(MemberStatus.PENDING_FOR_APPROVAL, MemberStatus.fromValue("PENDING_FOR_APPROVAL"));
        assertEquals(MemberStatus.PENDING_FOR_APPROVAL, MemberStatus.fromValue("pending_for_approval"));
        assertEquals(MemberStatus.JOINED, MemberStatus.fromValue("joined"));
    }

    @Test
    @DisplayName("fromValue should return null for invalid or null strings")
    void fromValue_InvalidString_ReturnsNull() {
        assertNull(MemberStatus.fromValue("UNKNOWN_STATUS"));
        assertNull(MemberStatus.fromValue("PENDING_APPROVAL"));
        assertNull(MemberStatus.fromValue(null));
        assertNull(MemberStatus.fromValue(""));
    }
}
