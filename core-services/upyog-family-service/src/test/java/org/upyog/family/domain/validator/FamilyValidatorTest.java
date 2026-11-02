package org.upyog.family.domain.validator;

import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.FileStoreClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDocument;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySplitRequest;
import org.egov.tracer.model.CustomException;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyValidatorTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FileStoreClient fileStoreClient;

    @Mock
    private FamilyMasterDataService familyMasterDataService;

    @InjectMocks
    private FamilyValidator validator;

    private Family family;
    private FamilyRequest request;

    @BeforeEach
    void setUp() {
        FamilyMember head = FamilyMember.builder()
                .userUuid("user-uuid-head")
                .relationshipWithHead("HEAD")
                .isHead(true)
                .active(true)
                .build();

        FamilyMember member = FamilyMember.builder()
                .userUuid("user-uuid-spouse")
                .relationshipWithHead("SPOUSE")
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
                .family(family)
                .build();
    }

    @Test
    @DisplayName("validateCreate should succeed for valid family unit")
    void validateCreate_Success() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> validator.validateCreate(request));
    }

    @Test
    @DisplayName("validateCreate should fail when relationship is invalid according to MDMS")
    void validateCreate_InvalidRelationship() {
        doThrow(new CustomException(FamilyConstants.ERROR_INVALID_RELATIONSHIP, "Invalid relationship"))
                .when(familyMasterDataService).validateRelationship(any(), eq("pg.citya"), eq("HEAD"));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_INVALID_RELATIONSHIP, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when tenantId is blank")
    void validateCreate_MissingTenantId() {
        family.setTenantId("");

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when no Head of Family exists")
    void validateCreate_NoHead() {
        family.getMembers().forEach(m -> m.setIsHead(false));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_HEAD_OF_FAMILY_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when multiple Heads of Family exist")
    void validateCreate_MultipleHeads() {
        family.getMembers().forEach(m -> m.setIsHead(true));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_HEAD_OF_FAMILY_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when headUserUuid mismatches isHead member UUID")
    void validateCreate_HeadUuidMismatch() {
        family.setHeadUserUuid("different-uuid");

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_HEAD_UUID_MISMATCH, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when member is already active in another family")
    void validateCreate_MemberAlreadyInAnotherFamily() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid("user-uuid-head"))
                .thenReturn(Collections.singletonList("FAM-EXISTING-001"));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when document filestoreId does not exist")
    void validateCreate_DocumentNotFound() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());
        when(fileStoreClient.validateFileExists("pg.citya", "invalid-store-id")).thenReturn(false);

        FamilyDocument doc = FamilyDocument.builder()
                .documentType("ADDRESS_PROOF")
                .filestoreId("invalid-store-id")
                .build();
        family.setDocuments(Collections.singletonList(doc));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_FILESTORE_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("validateSplit should succeed when splitting subset of active members")
    void validateSplit_Success() {
        FamilySplitRequest splitRequest = FamilySplitRequest.builder()
                .tenantId("pg.citya")
                .parentFamilyId("FAM-PARENT-001")
                .newFamilyHeadUserUuid("user-uuid-spouse")
                .splittingMemberUserUuids(Collections.singletonList("user-uuid-spouse"))
                .build();

        assertDoesNotThrow(() -> validator.validateSplit(splitRequest, family));
    }

    @Test
    @DisplayName("validateSplit should fail when trying to split all members")
    void validateSplit_CannotSplitAll() {
        FamilySplitRequest splitRequest = FamilySplitRequest.builder()
                .tenantId("pg.citya")
                .parentFamilyId("FAM-PARENT-001")
                .newFamilyHeadUserUuid("user-uuid-head")
                .splittingMemberUserUuids(Arrays.asList("user-uuid-head", "user-uuid-spouse"))
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateSplit(splitRequest, family));
        assertEquals(FamilyConstants.ERROR_INVALID_SPLIT, ex.getCode());
    }

    @Test
    @DisplayName("validateMerge should succeed for two distinct active families")
    void validateMerge_Success() {
        Family mergingFamily = Family.builder()
                .familyId("FAM-MERGE-002")
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-merge-head")
                .status(FamilyStatus.ACTIVE)
                .members(Collections.singletonList(FamilyMember.builder()
                        .userUuid("user-uuid-merge-head")
                        .active(true)
                        .isHead(true)
                        .build()))
                .build();
        family.setFamilyId("FAM-SURVIVING-001");

        FamilyMergeRequest mergeRequest = FamilyMergeRequest.builder()
                .tenantId("pg.citya")
                .survivingFamilyId("FAM-SURVIVING-001")
                .mergingFamilyId("FAM-MERGE-002")
                .commonHeadUserUuid("user-uuid-head")
                .build();

        assertDoesNotThrow(() -> validator.validateMerge(mergeRequest, family, mergingFamily));
    }

    @Test
    @DisplayName("validateMerge should fail when merging family into itself")
    void validateMerge_SelfMergeFails() {
        family.setFamilyId("FAM-001");
        FamilyMergeRequest mergeRequest = FamilyMergeRequest.builder()
                .tenantId("pg.citya")
                .survivingFamilyId("FAM-001")
                .mergingFamilyId("FAM-001")
                .commonHeadUserUuid("user-uuid-head")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateMerge(mergeRequest, family, family));
        assertEquals(FamilyConstants.ERROR_INVALID_MERGE, ex.getCode());
    }
}
