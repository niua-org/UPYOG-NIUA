package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.model.LineageActionType;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDashboardResponse;
import org.upyog.family.web.contract.FamilyDocument;
import org.upyog.family.web.contract.FamilyLineage;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilySearchCriteria;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyDashboardServiceImplTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private FamilyDashboardServiceImpl dashboardService;

    private Family activeFamily;

    @BeforeEach
    void setUp() {
        FamilyMember head = FamilyMember.builder()
                .userUuid("head-uuid-1")
                .isHead(true)
                .isVerified(true)
                .active(true)
                .build();

        FamilyMember member2 = FamilyMember.builder()
                .userUuid("member-uuid-2")
                .isHead(false)
                .isVerified(false)
                .active(true)
                .build();

        FamilyDocument doc = FamilyDocument.builder()
                .id("doc-1")
                .active(true)
                .build();

        activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .members(Arrays.asList(head, member2))
                .documents(Collections.singletonList(doc))
                .build();
    }

    @Test
    @DisplayName("Should aggregate dashboard metrics correctly")
    void getFamilyDashboard_Success() {
        FamilyLineage lineage = FamilyLineage.builder()
                .id("lin-1")
                .actionType(LineageActionType.SPLIT)
                .build();

        when(familyRepository.search(any(FamilySearchCriteria.class))).thenReturn(Collections.singletonList(activeFamily));
        when(familyRepository.findLineageByFamilyId("FAM-2026-0001")).thenReturn(Collections.singletonList(lineage));
        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), any(Boolean.class))).thenReturn(new ResponseInfo());

        FamilyDashboardResponse response = dashboardService.getFamilyDashboard("FAM-2026-0001", "pg.citya", new RequestInfo());

        assertNotNull(response);
        assertNotNull(response.getDashboard());
        assertEquals("FAM-2026-0001", response.getDashboard().getFamilyId());
        assertEquals(2, response.getDashboard().getTotalMembersCount());
        assertEquals(2, response.getDashboard().getActiveMembersCount());
        assertEquals(1, response.getDashboard().getVerifiedMembersCount());
        assertEquals(1, response.getDashboard().getDocumentsCount());
        assertEquals(1, response.getDashboard().getLineageEventsCount());
    }
}
