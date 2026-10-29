package org.upyog.family.domain.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.upyog.family.domain.model.ExitReason;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.LeaveFamilyRequest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaveFamilyValidatorTest {

    private LeaveFamilyValidator validator;
    private Family activeFamily;

    @BeforeEach
    void setUp() {
        validator = new LeaveFamilyValidator();

        FamilyMember head = FamilyMember.builder()
                .userUuid("head-uuid-1")
                .isHead(true)
                .active(true)
                .build();

        FamilyMember member2 = FamilyMember.builder()
                .userUuid("member-uuid-2")
                .isHead(false)
                .active(true)
                .build();

        activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .members(Arrays.asList(head, member2))
                .build();
    }

    @Test
    @DisplayName("Should pass when a regular active member leaves the family")
    void validate_Success() {
        LeaveFamilyRequest request = LeaveFamilyRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .memberUserUuid("member-uuid-2")
                .exitReason(ExitReason.MARRIAGE)
                .build();

        assertDoesNotThrow(() -> validator.validate(request, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception when HoF tries to leave while other members remain")
    void validate_HeadCannotLeaveWithoutTransfer() {
        LeaveFamilyRequest request = LeaveFamilyRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .memberUserUuid("head-uuid-1")
                .exitReason(ExitReason.RELOCATION)
                .build();

        assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception when target member is not in the family")
    void validate_MemberNotFound() {
        LeaveFamilyRequest request = LeaveFamilyRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .memberUserUuid("unknown-user")
                .exitReason(ExitReason.MARRIAGE)
                .build();

        assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
    }
}
