package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.validator.AssignHofValidator;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.web.contract.AssignHofRequest;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyHofCheckRequest;
import org.upyog.family.web.contract.FamilyHofCheckResponse;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilySearchCriteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyHeadServiceImplTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private AssignHofValidator assignHofValidator;

    @Mock
    private UserClient userClient;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private FamilyConfiguration configuration;

    @InjectMocks
    private FamilyHeadServiceImpl headService;

    private Family activeFamily;

    @BeforeEach
    void setUp() {
        FamilyMember head = FamilyMember.builder()
                .userUuid("head-uuid-1")
                .isHead(true)
                .active(true)
                .relationshipWithHead("HEAD")
                .auditDetails(AuditDetails.builder().createdBy("sys").build())
                .build();

        FamilyMember member2 = FamilyMember.builder()
                .userUuid("member-uuid-2")
                .isHead(false)
                .active(true)
                .relationshipWithHead("SPOUSE")
                .auditDetails(AuditDetails.builder().createdBy("sys").build())
                .build();

        activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .members(new ArrayList<>(Arrays.asList(head, member2)))
                .auditDetails(AuditDetails.builder().createdBy("system").createdTime(1000L).build())
                .build();

        org.mockito.Mockito.lenient().when(configuration.getAssignHofTopic()).thenReturn("assign-ug-family-hof");
    }

    @Test
    @DisplayName("Should transfer HoF role and re-point head flags")
    void assignHeadOfFamily_Success() {
        AssignHofRequest request = AssignHofRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .newHeadUserUuid("member-uuid-2")
                .reason("Senior citizen transfer")
                .build();

        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0001", "pg.citya")).thenReturn(Optional.of(activeFamily));

        Family result = headService.assignHeadOfFamily(request);

        assertNotNull(result);
        assertEquals("member-uuid-2", result.getHeadUserUuid());

        FamilyMember oldHead = result.getMembers().stream().filter(m -> "head-uuid-1".equals(m.getUserUuid())).findFirst().orElse(null);
        FamilyMember newHead = result.getMembers().stream().filter(m -> "member-uuid-2".equals(m.getUserUuid())).findFirst().orElse(null);

        assertNotNull(oldHead);
        assertNotNull(newHead);
        assertFalse(oldHead.getIsHead());
        assertTrue(newHead.getIsHead());
        assertEquals("HEAD", newHead.getRelationshipWithHead());
        verify(kafkaProducer).push(any(), any());
    }

    @Test
    @DisplayName("isHeadOfFamily: Should return isHof=true when logged-in user is the headUserUuid")
    void isHeadOfFamily_WhenCallerIsHoF_ReturnsTrue() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("head-uuid-1").tenantId("pg.citya").build())
                .build();
        FamilyHofCheckRequest request = FamilyHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .build();

        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0001", "pg.citya")).thenReturn(Optional.of(activeFamily));

        FamilyHofCheckResponse response = headService.isHeadOfFamily(request);

        assertNotNull(response);
        assertEquals("FAM-2026-0001", response.getFamilyId());
        assertEquals("head-uuid-1", response.getUserUuid());
        assertEquals("head-uuid-1", response.getHeadUserUuid());
        assertTrue(response.getIsHof());
    }

    @Test
    @DisplayName("isHeadOfFamily: Should return isHof=false when logged-in user is a non-head member")
    void isHeadOfFamily_WhenCallerIsNotHoF_ReturnsFalse() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("member-uuid-2").tenantId("pg.citya").build())
                .build();
        FamilyHofCheckRequest request = FamilyHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .build();

        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0001", "pg.citya")).thenReturn(Optional.of(activeFamily));

        FamilyHofCheckResponse response = headService.isHeadOfFamily(request);

        assertNotNull(response);
        assertEquals("FAM-2026-0001", response.getFamilyId());
        assertEquals("member-uuid-2", response.getUserUuid());
        assertEquals("head-uuid-1", response.getHeadUserUuid());
        assertFalse(response.getIsHof());
    }

    @Test
    @DisplayName("isHeadOfFamily: Should return isHof=false when caller does not belong to the family")
    void isHeadOfFamily_WhenCallerNotInFamily_ReturnsFalse() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("random-outsider-uuid").tenantId("pg.citya").build())
                .build();
        FamilyHofCheckRequest request = FamilyHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .build();

        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0001", "pg.citya")).thenReturn(Optional.of(activeFamily));

        FamilyHofCheckResponse response = headService.isHeadOfFamily(request);

        assertNotNull(response);
        assertEquals("random-outsider-uuid", response.getUserUuid());
        assertEquals("head-uuid-1", response.getHeadUserUuid());
        assertFalse(response.getIsHof());
    }

    @Test
    @DisplayName("isHeadOfFamily: Should throw CustomException when RequestInfo has no authenticated user")
    void isHeadOfFamily_WhenCallerUnauthenticated_ThrowsException() {
        FamilyHofCheckRequest request = FamilyHofCheckRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> headService.isHeadOfFamily(request));
        assertEquals("INVALID_REQUEST", ex.getCode());
    }

    @Test
    @DisplayName("isHeadOfFamily: Should throw CustomException when family does not exist")
    void isHeadOfFamily_WhenFamilyNotFound_ThrowsException() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("head-uuid-1").build())
                .build();
        FamilyHofCheckRequest request = FamilyHofCheckRequest.builder()
                .requestInfo(requestInfo)
                .familyId("NON-EXISTENT")
                .tenantId("pg.citya")
                .build();

        when(familyRepository.findByFamilyIdAndTenantId("NON-EXISTENT", "pg.citya")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> headService.isHeadOfFamily(request));
        assertEquals("FAMILY_NOT_FOUND", ex.getCode());
    }
}
