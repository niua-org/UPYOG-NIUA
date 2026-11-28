package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.GuardianRelation;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.asset.CitizenProfileDetail;
import org.egov.user.web.contract.asset.MemberAssetDetail;
import org.egov.user.web.contract.asset.MemberAssetSyncRequest;
import org.egov.user.web.contract.asset.MemberAssetSyncResponse;
import org.egov.user.web.contract.asset.MemberAssetSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test suite for {@link MemberAssetServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class MemberAssetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDocumentRepository documentRepository;

    private MemberAssetServiceImpl memberAssetService;

    @BeforeEach
    void setUp() {
        MemberAssetProcessor processor = new MemberAssetProcessor(
                userRepository,
                documentRepository
        );
        memberAssetService = new MemberAssetServiceImpl(processor);
    }

    private RequestInfo createTestRequestInfo() {
        return RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(1L)
                        .uuid("test-operator-uuid")
                        .build())
                .build();
    }

    @Test
    void syncMemberAssets_nullRequest_throwsException() {
        assertThrows(CustomException.class, () -> memberAssetService.syncMemberAssets(null));
    }

    @Test
    void syncMemberAssets_emptyMembers_returnsSuccessWithEmptyResults() {
        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(createTestRequestInfo())
                .members(Collections.emptyList())
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getResults().isEmpty());
    }

    @Test
    void syncMemberAssets_userNotFound_returnsFailedResult() {
        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid("non-existent-uuid")
                .tenantId("pg.citya")
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(createTestRequestInfo())
                .member(detail)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        MemberAssetSyncResult result = response.getResults().get(0);
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getMessage().contains("does not exist"));
    }

    @Test
    void syncMemberAssets_tenantMismatch_returnsFailedResult() {
        String citizenUuid = "citizen-uuid-tenant-test";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingUser));

        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pb.amritsar")
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(createTestRequestInfo())
                .member(detail)
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        MemberAssetSyncResult result = response.getResults().get(0);
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getMessage().contains("Tenant ID mismatch"));
    }

    @Test
    void syncMemberAssets_successfulSyncDemographicsAndDocuments() {
        String citizenUuid = "citizen-uuid-001";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .username("9876543210")
                .mobileNumber("9876543210")
                .type(UserType.CITIZEN)
                .fatherName("Old Father")
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingUser));

        CitizenProfileDetail citizenDetails = CitizenProfileDetail.builder()
                .uuid(citizenUuid)
                .fatherName("Ramesh Kumar")
                .motherName("Sita Devi")
                .spouseName("Anita Kumar")
                .caste("OBC")
                .isSpeciallyAbled(false)
                .guardian("Ramesh Kumar")
                .guardianRelation("FATHER")
                .alternateMobileNumber("9876543211")
                .emailId("citizen@example.com")
                .documents(Collections.singletonList(
                        UserDocument.builder()
                                .documentType("CASTE_CERTIFICATE")
                                .filestoreId("file-caste-123")
                                .build()
                ))
                .build();

        MemberAssetDetail memberDetail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .familyId("FAM-2026-001")
                .citizenDetails(citizenDetails)
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(createTestRequestInfo())
                .members(Collections.singletonList(memberDetail))
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(1, response.getResults().size());

        MemberAssetSyncResult result = response.getResults().get(0);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(citizenUuid, result.getUserUuid());
        assertEquals(1, result.getDocumentsSynced());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).update(userCaptor.capture(), any(), anyLong(), anyString());
        User updatedUser = userCaptor.getValue();
        assertEquals("Ramesh Kumar", updatedUser.getFatherName());
        assertEquals("Sita Devi", updatedUser.getMotherName());
        assertEquals("Anita Kumar", updatedUser.getSpouseName());
        assertEquals("OBC", updatedUser.getCaste());
        assertEquals(Boolean.FALSE, updatedUser.getIsSpeciallyAbled());
        assertEquals(GuardianRelation.FATHER, updatedUser.getGuardianRelation());

        verify(documentRepository, times(1)).create(any(UserDocument.class));
    }

    @Test
    void syncMemberAssets_singleMemberException_setsTopLevelStatusFailedAndSanitizesError() {
        String citizenUuid = "citizen-uuid-crash";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingUser));

        doThrow(new org.springframework.dao.DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint 'pg_catalog.idx_raw_sql'"))
                .when(documentRepository).create(any(UserDocument.class));

        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .documents(Collections.singletonList(
                        UserDocument.builder()
                                .documentType("AADHAAR")
                                .filestoreId("file-doc-1")
                                .build()))
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(createTestRequestInfo())
                .members(Collections.singletonList(detail))
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertEquals("FAILED", response.getResponseInfo().getStatus());
        assertEquals(1, response.getResults().size());

        MemberAssetSyncResult result = response.getResults().get(0);
        assertEquals("FAILED", result.getStatus());
        assertFalse(result.getMessage().contains("pg_catalog.idx_raw_sql"), "Raw SQL exception text must not leak");
        assertTrue(result.getMessage().contains("internal processing error"));
    }

    @Test
    void syncMemberAssets_partialFailure_setsTopLevelStatusPartialSuccess() {
        String uuidSuccess = "citizen-uuid-success";
        String uuidFail = "citizen-uuid-fail";

        User userSuccess = User.builder()
                .id(101L)
                .uuid(uuidSuccess)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        User userFail = User.builder()
                .id(102L)
                .uuid(uuidFail)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenAnswer(invocation -> {
                    UserSearchCriteria criteria = invocation.getArgument(0);
                    if (criteria.getUuid() != null && criteria.getUuid().contains(uuidSuccess)) {
                        return Collections.singletonList(userSuccess);
                    } else if (criteria.getUuid() != null && criteria.getUuid().contains(uuidFail)) {
                        return Collections.singletonList(userFail);
                    }
                    return Collections.emptyList();
                });

        MemberAssetDetail detail1 = MemberAssetDetail.builder()
                .userUuid(uuidSuccess)
                .tenantId("pg.citya")
                .build();

        MemberAssetDetail detail2 = MemberAssetDetail.builder()
                .userUuid(uuidFail)
                .tenantId("pb.amritsar")
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(createTestRequestInfo())
                .members(List.of(detail1, detail2))
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals("PARTIAL_SUCCESS", response.getStatus());
        assertEquals("PARTIAL_SUCCESS", response.getResponseInfo().getStatus());
        assertEquals(2, response.getResults().size());

        assertEquals("SUCCESS", response.getResults().get(0).getStatus());
        assertEquals("FAILED", response.getResults().get(1).getStatus());
    }

    @Test
    void syncMemberAssets_auditActor_usesCallerUserInfoWhenPresent() {
        String citizenUuid = "citizen-uuid-actor-001";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .fatherName("Old Father")
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingUser));

        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .fatherName("New Father")
                .build();

        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(777L)
                        .uuid("caller-employee-uuid")
                        .build())
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(requestInfo)
                .members(Collections.singletonList(detail))
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());

        verify(userRepository).update(any(User.class), any(User.class), eq(777L), eq("caller-employee-uuid"));
    }

    @Test
    void syncMemberAssets_auditActor_rejectsRequestWhenUserInfoAbsent() {
        String citizenUuid = "citizen-uuid-actor-002";
        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .fatherName("New Father")
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .members(Collections.singletonList(detail))
                .build();

        CustomException exception = assertThrows(CustomException.class, () ->
                memberAssetService.syncMemberAssets(request)
        );
        assertEquals("CALLER_INFO_REQUIRED", exception.getCode());
    }
}
