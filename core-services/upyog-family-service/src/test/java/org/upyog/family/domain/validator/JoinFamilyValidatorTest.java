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
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.JoinFamilyRequest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinFamilyValidatorTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyMasterDataService masterDataService;

    @Mock
    private RequesterDxClient requesterDxClient;

    @InjectMocks
    private JoinFamilyValidator validator;

    private Family activeFamily;

    @BeforeEach
    void setUp() {
        activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .build();
    }

    @Test
    @DisplayName("Should pass when citizen joins with valid relationship and HoF consent")
    void validate_Success() {
        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .hofConsentOtp("OTP-1234")
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("OTP-1234"), eq("applicant-uuid-5"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(request, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if applicant already in an active family")
    void validate_AlreadyInActiveFamily() {
        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .hofConsentOtp("OTP-1234")
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5"))
                .thenReturn(Collections.singletonList("FAM-2026-8888"));

        assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
    }
}
