package org.upyog.family.domain.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.web.contract.AssignHofRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssignHofValidatorTest {

    private AssignHofValidator validator;
    private Family activeFamily;

    @BeforeEach
    void setUp() {
        validator = new AssignHofValidator();

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
    @DisplayName("Should pass when assigning an existing active member as HoF")
    void validate_Success() {
        AssignHofRequest request = AssignHofRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .newHeadUserUuid("member-uuid-2")
                .build();

        assertDoesNotThrow(() -> validator.validate(request, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if new head is already the head")
    void validate_AlreadyHead() {
        AssignHofRequest request = AssignHofRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .newHeadUserUuid("head-uuid-1")
                .build();

        assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if new head is not a member of the family")
    void validate_MemberNotFound() {
        AssignHofRequest request = AssignHofRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .newHeadUserUuid("stranger-uuid-99")
                .build();

        assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
    }
}
