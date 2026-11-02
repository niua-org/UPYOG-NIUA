package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.validator.FamilyValidator;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.IdGenRepository;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.FamilySplitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyServiceImplTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyValidator familyValidator;

    @Mock
    private IdGenRepository idGenRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private FamilyConfiguration configuration;

    @InjectMocks
    private FamilyServiceImpl familyService;

    private Family family;
    private FamilyRequest request;
    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("test-user-uuid").build())
                .build();

        FamilyMember head = FamilyMember.builder()
                .userUuid("user-uuid-head")
                .relationshipWithHead("HEAD")
                .isHead(true)
                .active(true)
                .build();

        FamilyMember member = FamilyMember.builder()
                .userUuid("user-uuid-son")
                .relationshipWithHead("SON")
                .isHead(false)
                .active(true)
                .build();

        family = Family.builder()
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-head")
                .status(FamilyStatus.ACTIVE)
                .members(new ArrayList<>(Arrays.asList(head, member)))
                .build();

        request = FamilyRequest.builder()
                .requestInfo(requestInfo)
                .family(family)
                .build();
    }

    @Test
    @DisplayName("createFamily should generate family ID, persist, and publish to Kafka")
    void createFamily_Success() {
        when(idGenRepository.generateFamilyId(any(), eq("pg.citya"))).thenReturn("FAM-2026-0001");
        when(configuration.getCreateFamilyTopic()).thenReturn("save-ug-family");

        Map<String, CitizenProfile> profileMap = new HashMap<>();
        profileMap.put("user-uuid-head", CitizenProfile.builder().name("Rajesh Sharma").build());
        when(userClient.searchUsersByUuids(anyList(), any())).thenReturn(profileMap);

        Family result = familyService.createFamily(request);

        assertNotNull(result);
        assertEquals("FAM-2026-0001", result.getFamilyId());
        assertEquals(FamilyStatus.ACTIVE, result.getStatus());
        verify(kafkaProducer).push(eq("save-ug-family"), eq(request));
    }

    @Test
    @DisplayName("searchFamilies should query repository and enrich demographics")
    void searchFamilies_Success() {
        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .tenantId("pg.citya")
                .familyId("FAM-2026-0001")
                .build();

        family.setFamilyId("FAM-2026-0001");
        when(familyRepository.search(criteria)).thenReturn(Collections.singletonList(family));

        Map<String, CitizenProfile> profileMap = new HashMap<>();
        profileMap.put("user-uuid-head", CitizenProfile.builder().name("Rajesh Sharma").build());
        when(userClient.searchUsersByUuids(anyList(), any())).thenReturn(profileMap);

        List<Family> results = familyService.searchFamilies(criteria, requestInfo);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Rajesh Sharma", results.get(0).getMembers().get(0).getCitizenDetails().getName());
    }

    @Test
    @DisplayName("splitFamily should deactivate members from parent and create new child family unit")
    void splitFamily_Success() {
        family.setFamilyId("FAM-PARENT-001");
        family.setAuditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).build());

        when(familyRepository.search(any())).thenReturn(Collections.singletonList(family));
        when(idGenRepository.generateFamilyId(any(), eq("pg.citya"))).thenReturn("FAM-CHILD-002");
        when(configuration.getSplitFamilyTopic()).thenReturn("split-ug-family");

        FamilySplitRequest splitRequest = FamilySplitRequest.builder()
                .requestInfo(requestInfo)
                .tenantId("pg.citya")
                .parentFamilyId("FAM-PARENT-001")
                .newFamilyHeadUserUuid("user-uuid-son")
                .splittingMemberUserUuids(Collections.singletonList("user-uuid-son"))
                .reason("Marriage separation")
                .build();

        Family childFamily = familyService.splitFamily(splitRequest);

        assertNotNull(childFamily);
        assertEquals("FAM-CHILD-002", childFamily.getFamilyId());
        assertEquals("user-uuid-son", childFamily.getHeadUserUuid());
        verify(familyRepository).saveLineage(any());
        verify(kafkaProducer).push(eq("split-ug-family"), eq(splitRequest));
    }

    @Test
    @DisplayName("mergeFamily should merge members into surviving family and set merging family to MERGED")
    void mergeFamily_Success() {
        Family surviving = Family.builder()
                .familyId("FAM-001")
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-head")
                .status(FamilyStatus.ACTIVE)
                .members(new ArrayList<>(Collections.singletonList(FamilyMember.builder()
                        .userUuid("user-uuid-head")
                        .relationshipWithHead("HEAD")
                        .active(true)
                        .isHead(true)
                        .build())))
                .auditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).build())
                .build();

        Family merging = Family.builder()
                .familyId("FAM-002")
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-merge-head")
                .status(FamilyStatus.ACTIVE)
                .members(new ArrayList<>(Collections.singletonList(FamilyMember.builder()
                        .userUuid("user-uuid-merge-head")
                        .relationshipWithHead("HEAD")
                        .active(true)
                        .isHead(true)
                        .auditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).build())
                        .build())))
                .auditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).build())
                .build();

        when(familyRepository.search(any())).thenReturn(Collections.singletonList(surviving))
                .thenReturn(Collections.singletonList(merging));
        when(configuration.getMergeFamilyTopic()).thenReturn("merge-ug-family");

        FamilyMergeRequest mergeRequest = FamilyMergeRequest.builder()
                .requestInfo(requestInfo)
                .tenantId("pg.citya")
                .survivingFamilyId("FAM-001")
                .mergingFamilyId("FAM-002")
                .commonHeadUserUuid("user-uuid-head")
                .reason("Households joined")
                .build();

        Family mergedResult = familyService.mergeFamily(mergeRequest);

        assertNotNull(mergedResult);
        assertEquals("FAM-001", mergedResult.getFamilyId());
        assertEquals(2, mergedResult.getMembers().size());
        assertEquals(FamilyStatus.MERGED, merging.getStatus());
        verify(familyRepository).saveLineage(any());
        verify(kafkaProducer).push(eq("merge-ug-family"), eq(mergeRequest));
    }
}
