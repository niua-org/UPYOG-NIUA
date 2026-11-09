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
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDashboardResponse;
import org.upyog.family.web.contract.FamilyDocument;
import org.upyog.family.web.contract.FamilyLineage;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilySearchCriteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    @DisplayName("Should aggregate dashboard metrics correctly with enriched member display data")
    void getFamilyDashboard_Success() {
        FamilyLineage lineage = FamilyLineage.builder()
                .id("lin-1")
                .actionType(LineageActionType.SPLIT)
                .build();

        CitizenProfile headProfile = CitizenProfile.builder()
                .name("Rajesh Sharma")
                .photo("photo-file-id-123")
                .mobileNumber("9876543210")
                .gender("MALE")
                .dob(631152000000L)
                .relationshipWithHead("HEAD")
                .build();

        CitizenProfile member2Profile = CitizenProfile.builder()
                .name("Aarav Sharma")
                .photo("photo-file-id-456")
                .mobileNumber("9876543211")
                .gender("MALE")
                .dob(946684800000L)
                .relationshipWithHead("SON")
                .build();

        Map<String, CitizenProfile> profileMap = new HashMap<>();
        profileMap.put("head-uuid-1", headProfile);
        profileMap.put("member-uuid-2", member2Profile);

        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0001", "pg.citya")).thenReturn(Optional.of(activeFamily));
        when(familyRepository.findLineageByFamilyId("FAM-2026-0001")).thenReturn(Collections.singletonList(lineage));
        when(userClient.searchUsersByUuids(anyList(), any())).thenReturn(profileMap);
        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), any(Boolean.class))).thenReturn(new ResponseInfo());

        FamilyDashboardResponse response = dashboardService.getFamilyDashboard("FAM-2026-0001", "pg.citya", new RequestInfo());

        assertNotNull(response);
        assertNotNull(response.getDashboard());
        assertEquals("FAM-2026-0001", response.getDashboard().getFamilyId());
        assertEquals("Rajesh Sharma", response.getDashboard().getHeadName());
        assertEquals(2, response.getDashboard().getTotalMembersCount());
        assertEquals(2, response.getDashboard().getActiveMembersCount());
        assertEquals(1, response.getDashboard().getVerifiedMembersCount());
        assertEquals(1, response.getDashboard().getDocumentsCount());
        assertEquals(1, response.getDashboard().getLineageEventsCount());

        assertNotNull(response.getDashboard().getActiveMembers());
        assertEquals(2, response.getDashboard().getActiveMembers().size());

        FamilyMember headMember = response.getDashboard().getActiveMembers().get(0);
        assertEquals("Rajesh Sharma", headMember.getName());
        assertEquals("photo-file-id-123", headMember.getPhoto());
        assertEquals("9876543210", headMember.getMobileNumber());
        assertEquals("MALE", headMember.getGender());
        assertEquals(631152000000L, headMember.getDob());

        FamilyMember secondMember = response.getDashboard().getActiveMembers().get(1);
        assertEquals("Aarav Sharma", secondMember.getName());
        assertEquals("photo-file-id-456", secondMember.getPhoto());
        assertEquals("9876543211", secondMember.getMobileNumber());
        assertEquals("SON", secondMember.getRelationshipWithHead());
    }

    @Test
    @DisplayName("Should synthesize Head of Family into activeMembers when head is missing from member list")
    void getFamilyDashboard_SynthesizesHeadWhenMissingFromMembers() {
        FamilyMember memberOnly = FamilyMember.builder()
                .id("MEM-001")
                .userUuid("member-uuid-2")
                .relationshipWithHead("FATHER")
                .isHead(false)
                .isVerified(true)
                .active(true)
                .build();

        Family familyWithoutHeadInMembers = Family.builder()
                .familyId("FAM-2026-0002")
                .tenantId("pg")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .members(new java.util.ArrayList<>(Collections.singletonList(memberOnly)))
                .documents(Collections.emptyList())
                .build();

        CitizenProfile headProfile = CitizenProfile.builder()
                .name("Head Of Family")
                .photo("photo-head")
                .mobileNumber("9999999999")
                .gender("FEMALE")
                .build();

        CitizenProfile memberProfile = CitizenProfile.builder()
                .name("Father Member")
                .mobileNumber("9716734311")
                .gender("MALE")
                .relationshipWithHead("FATHER")
                .build();

        Map<String, CitizenProfile> profileMap = new HashMap<>();
        profileMap.put("head-uuid-1", headProfile);
        profileMap.put("member-uuid-2", memberProfile);

        when(familyRepository.findByFamilyIdAndTenantId("FAM-2026-0002", "pg")).thenReturn(Optional.of(familyWithoutHeadInMembers));
        when(familyRepository.findLineageByFamilyId("FAM-2026-0002")).thenReturn(Collections.emptyList());
        when(userClient.searchUsersByUuids(anyList(), any())).thenReturn(profileMap);
        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), any(Boolean.class))).thenReturn(new ResponseInfo());

        FamilyDashboardResponse response = dashboardService.getFamilyDashboard("FAM-2026-0002", "pg", new RequestInfo());

        assertNotNull(response);
        assertNotNull(response.getDashboard());
        assertEquals("Head Of Family", response.getDashboard().getHeadName());
        assertEquals(2, response.getDashboard().getActiveMembersCount());
        assertEquals(2, response.getDashboard().getActiveMembers().size());

        // First member should be the synthesized Head of Family
        FamilyMember head = response.getDashboard().getActiveMembers().get(0);
        assertEquals("head-uuid-1", head.getUserUuid());
        assertEquals("Head Of Family", head.getName());
        assertEquals("HEAD", head.getRelationshipWithHead());
        org.junit.jupiter.api.Assertions.assertTrue(Boolean.TRUE.equals(head.getIsHead()));

        // Second member is the father
        FamilyMember father = response.getDashboard().getActiveMembers().get(1);
        assertEquals("member-uuid-2", father.getUserUuid());
        assertEquals("Father Member", father.getName());
    }
}
