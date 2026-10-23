package org.upyog.family.domain.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.RequesterDxClient;
import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddMemberValidatorTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyMasterDataService masterDataService;

    @Mock
    private RequesterDxClient requesterDxClient;

    @InjectMocks
    private AddMemberValidator validator;

    private Family activeFamily;
    private AddMemberRequest validRequest;

    @BeforeEach
    void setUp() {
        FamilyMember head = FamilyMember.builder()
                .userUuid("head-uuid-1")
                .isHead(true)
                .active(true)
                .build();

        activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .members(new ArrayList<>(Collections.singletonList(head)))
                .build();

        FamilyMember newMember = FamilyMember.builder()
                .userUuid("member-uuid-2")
                .relationshipWithHead("SPOUSE")
                .isHead(false)
                .active(true)
                .build();

        validRequest = AddMemberRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .member(newMember)
                .consentToken("CONSENT-VERIFIED-12345")
                .build();
    }

    @Test
    @DisplayName("Should pass validation when all rules and Aadhaar consent succeed")
    void validate_Success() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("pg.citya"), any())).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(validRequest, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if family does not exist")
    void validate_FamilyNotFound() {
        assertThrows(CustomException.class, () -> validator.validate(validRequest, null));
    }

    @Test
    @DisplayName("Should throw exception if family is not active")
    void validate_FamilyInactive() {
        activeFamily.setStatus(FamilyStatus.SPLIT);
        assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if citizen is already active in another family")
    void validate_CitizenAlreadyInAnotherFamily() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2"))
                .thenReturn(Collections.singletonList("FAM-2026-9999"));

        assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if Aadhaar consent is invalid")
    void validate_ConsentInvalid() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("pg.citya"), any())).thenReturn(false);

        assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
    }
}
