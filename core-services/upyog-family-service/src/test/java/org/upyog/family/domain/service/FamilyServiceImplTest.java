package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.validator.FamilyValidator;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.IdGenRepository;
import org.upyog.family.persistence.repository.OtpClient;
import org.upyog.family.persistence.repository.RequesterDxClient;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.egov.tracer.model.CustomException;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.FamilySplitRequest;
import org.upyog.family.web.contract.UserVehicleDetail;
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
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
        verify(userClient).syncMemberAssets(eq(family.getMembers()), eq("pg.citya"), eq(requestInfo));
    }

    /**
     * Fix for PR Comment #10:
     * When members declare assets and sync to egov-user fails while failOnAssetSyncError is true (default),
     * createFamily must throw CustomException and must NOT dispatch family creation to Kafka.
     */
    @Test
    @DisplayName("createFamily should throw CustomException and abort Kafka push when asset sync fails and failOnAssetSyncError is true")
    void createFamily_MemberAssetSyncFailure_ThrowsException() {
        when(idGenRepository.generateFamilyId(any(), eq("pg.citya"))).thenReturn("FAM-2026-0001");
        when(configuration.getFailOnAssetSyncError()).thenReturn(true);

        // Member declares a vehicle
        family.getMembers().get(0).setVehicles(Collections.singletonList(
                UserVehicleDetail.builder().vehicleNumber("KA01AB1234").build()
        ));

        // Asset sync fails
        when(userClient.syncMemberAssets(anyList(), eq("pg.citya"), any())).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class, () -> familyService.createFamily(request));
        assertEquals(FamilyConstants.ERROR_MEMBER_ASSET_SYNC_FAILED, ex.getCode());

        // Verify Kafka producer is never called when sync fails with failOnAssetSyncError enabled
        verify(kafkaProducer, never()).push(any(), any());
    }

    /**
     * Fix for PR Comment #10:
     * When members declare assets and sync succeeds, status should be recorded as SUCCESS and Kafka push occurs.
     */
    @Test
    @DisplayName("createFamily should record SUCCESS status and push to Kafka when asset sync succeeds")
    void createFamily_MemberAssetSyncSuccess_UpdatesStatus() {
        when(idGenRepository.generateFamilyId(any(), eq("pg.citya"))).thenReturn("FAM-2026-0001");
        when(configuration.getCreateFamilyTopic()).thenReturn("save-ug-family");

        Map<String, CitizenProfile> profileMap = new HashMap<>();
        profileMap.put("user-uuid-head", CitizenProfile.builder().name("Rajesh Sharma").build());
        when(userClient.searchUsersByUuids(anyList(), any())).thenReturn(profileMap);

        family.getMembers().get(0).setVehicles(Collections.singletonList(
                UserVehicleDetail.builder()
                        .vehicleNumber("KA01AB1234")
                        .fuelType("PETROL")
                        .category("4_WHEELER")
                        .build()
        ));

        when(userClient.syncMemberAssets(anyList(), eq("pg.citya"), any())).thenReturn(true);

        Family result = familyService.createFamily(request);

        assertNotNull(result);
        verify(kafkaProducer).push(eq("save-ug-family"), eq(request));
        verify(userClient).syncMemberAssets(argThat(members ->
                members.get(0).getVehicles().get(0).getFuelType().equals("PETROL") &&
                members.get(0).getVehicles().get(0).getCategory().equals("4_WHEELER")
        ), eq("pg.citya"), any());
        Map<String, Object> details = (Map<String, Object>) result.getAdditionalDetails();
        assertNotNull(details);
        assertEquals(FamilyConstants.STATUS_SUCCESS, details.get(FamilyConstants.KEY_MEMBER_ASSET_SYNC_STATUS));
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
        profileMap.put("user-uuid-head", CitizenProfile.builder()
                .name("Rajesh Sharma")
                .photo("photo-head-001")
                .mobileNumber("9876543210")
                .gender("MALE")
                .dob(631152000000L)
                .relationshipWithHead("HEAD")
                .build());
        when(userClient.searchUsersByUuids(anyList(), any())).thenReturn(profileMap);

        List<Family> results = familyService.searchFamilies(criteria, requestInfo);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Rajesh Sharma", results.get(0).getHeadName());
        assertEquals("Rajesh Sharma", results.get(0).getMembers().get(0).getName());
        assertEquals("photo-head-001", results.get(0).getMembers().get(0).getPhoto());
        assertEquals("9876543210", results.get(0).getMembers().get(0).getMobileNumber());
        assertEquals("MALE", results.get(0).getMembers().get(0).getGender());
        assertEquals(631152000000L, results.get(0).getMembers().get(0).getDob());
        assertEquals("HEAD", results.get(0).getMembers().get(0).getRelationshipWithHead());
        assertEquals("Rajesh Sharma", results.get(0).getMembers().get(0).getCitizenDetails().getName());
    }

    @Test
    @DisplayName("splitFamily should deactivate members from parent and create new child family unit")
    void splitFamily_Success() {
        family.setFamilyId("FAM-PARENT-001");
        family.setAuditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).build());

        when(familyRepository.findByFamilyIdAndTenantId("FAM-PARENT-001", "pg.citya")).thenReturn(Optional.of(family));
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

        when(familyRepository.findByFamilyIdAndTenantId("FAM-001", "pg.citya")).thenReturn(Optional.of(surviving));
        when(familyRepository.findByFamilyIdAndTenantId("FAM-002", "pg.citya")).thenReturn(Optional.of(merging));
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

    /**
     * Fix for PR Comment #11 (FamilyServiceImpl.java:338):
     * Verifies that updateFamily only synchronizes members whose assets/demographics were actually modified,
     * rather than re-syncing every member in the household.
     */
    @Test
    @DisplayName("updateFamily should only synchronize members whose assets or demographics were actually modified")
    void updateFamily_SyncsOnlyModifiedMemberAssets() {
        family.setFamilyId("FAM-2026-0001");
        family.setAuditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).lastModifiedTime(100L).build());

        Family existingFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-head")
                .status(FamilyStatus.ACTIVE)
                .members(new ArrayList<>(Arrays.asList(
                        FamilyMember.builder().userUuid("user-uuid-head").relationshipWithHead("HEAD").isHead(true).active(true).build(),
                        FamilyMember.builder().userUuid("user-uuid-son").relationshipWithHead("SON").isHead(false).active(true).build()
                )))
                .auditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).lastModifiedTime(100L).build())
                .build();

        when(familyRepository.search(any())).thenReturn(Collections.singletonList(existingFamily));
        when(configuration.getUpdateFamilyTopic()).thenReturn("update-ug-family");

        // Incoming update payload: Head has NO declared assets, Son declares a new vehicle
        FamilyMember updatedHead = FamilyMember.builder()
                .userUuid("user-uuid-head")
                .relationshipWithHead("HEAD")
                .isHead(true)
                .active(true)
                .build();
        FamilyMember updatedSon = FamilyMember.builder()
                .userUuid("user-uuid-son")
                .relationshipWithHead("SON")
                .isHead(false)
                .active(true)
                .vehicles(Collections.singletonList(UserVehicleDetail.builder().vehicleNumber("DL01XY9999").build()))
                .build();

        family.setMembers(new ArrayList<>(Arrays.asList(updatedHead, updatedSon)));

        when(userClient.syncMemberAssets(anyList(), eq("pg.citya"), any())).thenReturn(true);

        Family result = familyService.updateFamily(request);

        assertNotNull(result);
        // Verify only the Son was passed to syncMemberAssets (Head was excluded because no assets were declared/modified)
        verify(userClient).syncMemberAssets(
                argThat(list -> list.size() == 1 && "user-uuid-son".equals(list.get(0).getUserUuid())),
                eq("pg.citya"),
                eq(requestInfo)
        );
        verify(kafkaProducer).push(eq("update-ug-family"), any());
    }

    /**
     * Fix for PR Comment #11 (FamilyServiceImpl.java:338):
     * Verifies that updateFamily completely skips asset synchronization when no members declare or modify assets.
     */
    @Test
    @DisplayName("updateFamily should skip asset synchronization when no members have declared assets")
    void updateFamily_SkipsSyncWhenNoMemberAssetsModified() {
        family.setFamilyId("FAM-2026-0001");
        family.setCategory("OBC");
        family.setAuditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).lastModifiedTime(100L).build());

        Family existingFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-head")
                .category("GENERAL")
                .status(FamilyStatus.ACTIVE)
                .members(new ArrayList<>(Arrays.asList(
                        FamilyMember.builder().userUuid("user-uuid-head").relationshipWithHead("HEAD").isHead(true).active(true).build(),
                        FamilyMember.builder().userUuid("user-uuid-son").relationshipWithHead("SON").isHead(false).active(true).build()
                )))
                .auditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).lastModifiedTime(100L).build())
                .build();

        when(familyRepository.search(any())).thenReturn(Collections.singletonList(existingFamily));
        when(configuration.getUpdateFamilyTopic()).thenReturn("update-ug-family");

        Family result = familyService.updateFamily(request);

        assertNotNull(result);
        assertEquals("OBC", result.getCategory());
        // Verify syncMemberAssets was NEVER called because no members declared assets
        verify(userClient, never()).syncMemberAssets(anyList(), any(), any());
        verify(kafkaProducer).push(eq("update-ug-family"), any());
    }

    /**
     * Fix for PR Comment #10 & #11:
     * When a modified member's asset sync fails and failOnAssetSyncError is true, updateFamily must throw CustomException
     * and must NOT dispatch family update to Kafka persister.
     */
    @Test
    @DisplayName("updateFamily should throw CustomException and abort Kafka push when modified member asset sync fails")
    void updateFamily_ThrowsExceptionWhenModifiedAssetSyncFails() {
        family.setFamilyId("FAM-2026-0001");
        family.setAuditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).lastModifiedTime(100L).build());

        Family existingFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-head")
                .status(FamilyStatus.ACTIVE)
                .members(new ArrayList<>(Arrays.asList(
                        FamilyMember.builder().userUuid("user-uuid-head").relationshipWithHead("HEAD").isHead(true).active(true).build(),
                        FamilyMember.builder().userUuid("user-uuid-son").relationshipWithHead("SON").isHead(false).active(true).build()
                )))
                .auditDetails(AuditDetails.builder().createdBy("sys").createdTime(100L).lastModifiedTime(100L).build())
                .build();

        when(familyRepository.search(any())).thenReturn(Collections.singletonList(existingFamily));
        when(configuration.getFailOnAssetSyncError()).thenReturn(true);

        FamilyMember updatedSon = FamilyMember.builder()
                .userUuid("user-uuid-son")
                .relationshipWithHead("SON")
                .isHead(false)
                .active(true)
                .vehicles(Collections.singletonList(UserVehicleDetail.builder().vehicleNumber("DL01XY9999").build()))
                .build();
        family.setMembers(new ArrayList<>(Collections.singletonList(updatedSon)));

        when(userClient.syncMemberAssets(anyList(), eq("pg.citya"), any())).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class, () -> familyService.updateFamily(request));
        assertEquals(FamilyConstants.ERROR_MEMBER_ASSET_SYNC_FAILED, ex.getCode());

        verify(kafkaProducer, never()).push(any(), any());
    }
}
