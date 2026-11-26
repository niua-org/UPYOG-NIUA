package org.upyog.draft.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.draft.repository.DraftRepository;
import org.upyog.draft.util.DraftConstants;
import org.upyog.draft.web.models.DraftDetail;
import org.upyog.draft.web.models.DraftRequest;
import org.upyog.draft.web.models.DraftResponse;
import org.upyog.draft.web.models.DraftSearchCriteria;
import org.upyog.draft.web.models.DraftSearchRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DraftService}.
 *
 * <p>DraftService's only collaborator is {@link DraftRepository}. It does not
 * inject a Kafka producer, so the outbound payload captured here is the
 * {@link DraftRequest} passed into the repository.
 */
@ExtendWith(MockitoExtension.class)
class DraftServiceTest {

    private static final String USER_UUID = "user-1";

    @Mock
    private DraftRepository draftRepository;

    @InjectMocks
    private DraftService draftService;

    @Captor
    private ArgumentCaptor<DraftRequest> payloadCaptor;

    @Test
    void save_newDraft_persistsActiveDraftWithGeneratedId() {
        DraftDetail draft = savableDraft();
        draft.setCreatorType("employee");
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);

        DraftResponse response = draftService.save(request);

        verify(draftRepository).save(payloadCaptor.capture(), eq(true));
        DraftDetail saved = payloadCaptor.getValue().getDraft();
        assertThat(saved.getDraftId()).isNotBlank();
        UUID.fromString(saved.getDraftId());
        assertThat(saved.getCreatedBy()).isEqualTo(USER_UUID);
        assertThat(saved.getLastModifiedBy()).isEqualTo(USER_UUID);
        assertThat(saved.getCreatorType()).isEqualTo("EMPLOYEE");
        assertThat(saved.getStatus()).isEqualTo(DraftConstants.STATUS_ACTIVE);
        assertThat(saved.getCompletionPct()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getCreatedTime()).isNotNull().isEqualTo(saved.getLastModifiedTime());
        assertThat(response.getDraft()).isSameAs(draft);
        assertThat(response.getResponseInfo().getStatus()).isEqualTo("successful");
        assertThat(response.getResponseInfo().getApiId()).isEqualTo("draft");
        assertThat(response.getDrafts()).isNull();
        assertThat(response.getCount()).isNull();
    }

    @Test
    void save_nullDraft_throwsDraftRequired() {
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), null);

        assertThatThrownBy(() -> draftService.save(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Draft payload is required")
                .hasFieldOrPropertyWithValue("code", "DRAFT_REQUIRED");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void save_blankBusinessService_throwsBusinessServiceRequired() {
        DraftDetail draft = savableDraft();
        draft.setBusinessService("  ");
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);

        assertThatThrownBy(() -> draftService.save(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("businessService is required")
                .hasFieldOrPropertyWithValue("code", "BUSINESS_SERVICE_REQUIRED");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void save_blankTenantId_throwsTenantIdRequired() {
        DraftDetail draft = savableDraft();
        draft.setTenantId(" ");
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);

        assertThatThrownBy(() -> draftService.save(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("tenantId is required")
                .hasFieldOrPropertyWithValue("code", "TENANT_ID_REQUIRED");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void save_nullDraftData_throwsDraftDataRequired() {
        DraftDetail draft = savableDraft();
        draft.setDraftData(null);
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);

        assertThatThrownBy(() -> draftService.save(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("draftData is required")
                .hasFieldOrPropertyWithValue("code", "DRAFT_DATA_REQUIRED");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void save_unknownCreatorType_usesEmployeeFromAuthenticatedUser() {
        DraftDetail draft = savableDraft();
        draft.setCreatorType("citizen");
        DraftRequest request = saveRequest(caller(USER_UUID, "EMPLOYEE"), draft);

        draftService.save(request);

        verify(draftRepository).save(payloadCaptor.capture(), eq(true));
        assertThat(payloadCaptor.getValue().getDraft().getCreatorType()).isEqualTo("EMPLOYEE");
    }

    @Test
    void save_blankCreatorTypeWithCitizenUser_defaultsToUser() {
        DraftDetail draft = savableDraft();
        draft.setCreatorType(null);
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);

        draftService.save(request);

        verify(draftRepository).save(payloadCaptor.capture(), eq(true));
        assertThat(payloadCaptor.getValue().getDraft().getCreatorType()).isEqualTo("USER");
    }

    @Test
    void save_missingUserInfo_throwsUserInfoRequired() {
        DraftRequest request = saveRequest(RequestInfo.builder().apiId("draft").build(), savableDraft());

        assertThatThrownBy(() -> draftService.save(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("user UUID is required")
                .hasFieldOrPropertyWithValue("code", "USER_INFO_REQUIRED");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void save_createdByMismatch_throwsUnauthorized() {
        DraftDetail draft = savableDraft();
        draft.setCreatedBy("someone-else");
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);

        assertThatThrownBy(() -> draftService.save(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("another user")
                .hasFieldOrPropertyWithValue("code", "UNAUTHORIZED_DRAFT_ACCESS");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void save_existingDraftMissing_throwsDraftNotFound() {
        DraftDetail draft = savableDraft();
        draft.setDraftId("draft-404");
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);
        when(draftRepository.findByDraftId("draft-404", "pb.amritsar", USER_UUID)).thenReturn(null);

        assertThatThrownBy(() -> draftService.save(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("draft-404")
                .hasFieldOrPropertyWithValue("code", "DRAFT_NOT_FOUND");
        verify(draftRepository, never()).save(any(), anyBoolean());
    }

    @Test
    void save_existingDraft_overwritesCreatedByWithCallerAndPersistsUpdate() {
        DraftDetail draft = savableDraft();
        draft.setDraftId("draft-1");
        draft.setCompletionPct(new BigDecimal("15"));
        DraftDetail existing = savableDraft();
        existing.setDraftId("draft-1");
        existing.setCreatedBy("original-owner");
        existing.setCreatedTime(1_000L);
        existing.setCreatorType("EMPLOYEE");
        existing.setStatus(DraftConstants.STATUS_ACTIVE);
        when(draftRepository.findByDraftId("draft-1", "pb.amritsar", USER_UUID)).thenReturn(existing);
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);

        DraftResponse response = draftService.save(request);

        verify(draftRepository).save(payloadCaptor.capture(), eq(false));
        DraftDetail saved = payloadCaptor.getValue().getDraft();
        assertThat(saved.getCreatedBy()).isEqualTo(USER_UUID);
        assertThat(saved.getLastModifiedBy()).isEqualTo(USER_UUID);
        assertThat(saved.getCreatedTime()).isEqualTo(1_000L);
        assertThat(saved.getLastModifiedTime()).isGreaterThan(1_000L);
        assertThat(saved.getCreatorType()).isEqualTo("EMPLOYEE");
        assertThat(saved.getStatus()).isEqualTo(DraftConstants.STATUS_ACTIVE);
        assertThat(saved.getCompletionPct()).isEqualByComparingTo(new BigDecimal("15"));
        assertThat(saved.getDraftId()).isEqualTo("draft-1");
        assertThat(response.getDraft()).isSameAs(draft);
        assertThat(response.getResponseInfo().getStatus()).isEqualTo("successful");
    }

    @Test
    void search_statusAndPagingMissing_appliesDefaultsAndReturnsRows() {
        DraftSearchCriteria criteria = DraftSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .build();
        DraftSearchRequest request = DraftSearchRequest.builder()
                .requestInfo(caller(USER_UUID, "CITIZEN"))
                .criteria(criteria)
                .build();
        DraftDetail found = savableDraft();
        found.setDraftId("draft-1");
        when(draftRepository.search(criteria)).thenReturn(List.of(found));

        DraftResponse response = draftService.search(request);

        assertThat(criteria.getCreatedBy()).isEqualTo(USER_UUID);
        assertThat(criteria.getStatus()).isEqualTo(DraftConstants.STATUS_ACTIVE);
        assertThat(criteria.getOffset()).isEqualTo(0);
        assertThat(criteria.getLimit()).isEqualTo(10);
        assertThat(response.getDrafts()).containsExactly(found);
        assertThat(response.getItems()).containsExactly(found);
        assertThat(response.getResponseInfo().getStatus()).isEqualTo("successful");
        assertThat(response.getResponseInfo().getMsgId()).isEqualTo("m-1");
    }

    @Test
    void search_repositoryReturnsEmpty_returnsEmptyDraftsAndItems() {
        DraftSearchCriteria criteria = DraftSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .status(DraftConstants.STATUS_DISCARDED)
                .offset(5)
                .limit(2)
                .build();
        DraftSearchRequest request = DraftSearchRequest.builder()
                .requestInfo(caller(USER_UUID, "USER"))
                .criteria(criteria)
                .build();
        when(draftRepository.search(criteria)).thenReturn(List.of());

        DraftResponse response = draftService.search(request);

        assertThat(criteria.getStatus()).isEqualTo(DraftConstants.STATUS_DISCARDED);
        assertThat(criteria.getOffset()).isEqualTo(5);
        assertThat(criteria.getLimit()).isEqualTo(2);
        assertThat(response.getDrafts()).isEmpty();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void search_nullCriteria_throwsCriteriaRequired() {
        DraftSearchRequest request = DraftSearchRequest.builder()
                .requestInfo(caller(USER_UUID, "CITIZEN"))
                .build();

        assertThatThrownBy(() -> draftService.search(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("DraftSearchCriteria is required")
                .hasFieldOrPropertyWithValue("code", "DRAFT_SEARCH_CRITERIA_REQUIRED");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void count_criteriaPresent_returnsCount() {
        DraftSearchCriteria criteria = DraftSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .build();
        DraftSearchRequest request = DraftSearchRequest.builder()
                .requestInfo(caller(USER_UUID, "CITIZEN"))
                .criteria(criteria)
                .build();
        when(draftRepository.count(criteria)).thenReturn(4);

        DraftResponse response = draftService.count(request);

        assertThat(criteria.getCreatedBy()).isEqualTo(USER_UUID);
        assertThat(criteria.getStatus()).isEqualTo(DraftConstants.STATUS_ACTIVE);
        assertThat(response.getCount()).isEqualTo(4);
        assertThat(response.getDraft()).isNull();
        assertThat(response.getResponseInfo().getStatus()).isEqualTo("successful");
    }

    @Test
    void count_nullCriteria_throwsCriteriaRequired() {
        DraftSearchRequest request = DraftSearchRequest.builder()
                .requestInfo(caller(USER_UUID, "CITIZEN"))
                .build();

        assertThatThrownBy(() -> draftService.count(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("DraftSearchCriteria is required")
                .hasFieldOrPropertyWithValue("code", "DRAFT_SEARCH_CRITERIA_REQUIRED");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void delete_blankDraftId_throwsDraftIdRequired() {
        DraftDetail draft = savableDraft();
        draft.setDraftId(" ");
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);

        assertThatThrownBy(() -> draftService.delete(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("draftId is required")
                .hasFieldOrPropertyWithValue("code", "DRAFT_ID_REQUIRED");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void delete_draftIdPresent_sendsDiscardedPayload() {
        DraftDetail draft = savableDraft();
        draft.setDraftId("draft-1");
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), draft);

        DraftResponse response = draftService.delete(request);

        verify(draftRepository).updateStatus(payloadCaptor.capture());
        DraftDetail sent = payloadCaptor.getValue().getDraft();
        assertThat(sent.getStatus()).isEqualTo(DraftConstants.STATUS_DISCARDED);
        assertThat(sent.getCreatedBy()).isEqualTo(USER_UUID);
        assertThat(sent.getLastModifiedBy()).isEqualTo(USER_UUID);
        assertThat(sent.getLastModifiedTime()).isPositive();
        assertThat(response.getDraft().getStatus()).isEqualTo(DraftConstants.STATUS_DISCARDED);
        assertThat(response.getResponseInfo().getStatus()).isEqualTo("successful");
    }

    @Test
    void markSubmitted_nullDraft_throwsDraftIdRequired() {
        DraftRequest request = saveRequest(caller(USER_UUID, "CITIZEN"), null);

        assertThatThrownBy(() -> draftService.markSubmitted(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("draftId is required")
                .hasFieldOrPropertyWithValue("code", "DRAFT_ID_REQUIRED");
        verifyNoInteractions(draftRepository);
    }

    @Test
    void markSubmitted_draftIdPresent_sendsSubmittedPayload() {
        DraftDetail draft = savableDraft();
        draft.setDraftId("draft-9");
        DraftRequest request = saveRequest(caller(USER_UUID, "EMPLOYEE"), draft);

        DraftResponse response = draftService.markSubmitted(request);

        verify(draftRepository).updateStatus(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getDraft().getStatus()).isEqualTo(DraftConstants.STATUS_SUBMITTED);
        assertThat(payloadCaptor.getValue().getDraft().getLastModifiedBy()).isEqualTo(USER_UUID);
        assertThat(response.getDraft().getDraftId()).isEqualTo("draft-9");
        assertThat(response.getResponseInfo().getApiId()).isEqualTo("draft");
    }

    @Test
    void purgeActiveOlderThan_noneStale_returnsZero() {
        when(draftRepository.findActiveDraftsOlderThan(50L)).thenReturn(List.of());

        assertThat(draftService.purgeActiveOlderThan(50L)).isZero();
        verify(draftRepository, never()).updateStatus(any());
    }

    @Test
    void purgeActiveOlderThan_staleDrafts_discardsEachAndReturnsSize() {
        DraftDetail first = staleDraft("d-1", "owner-a");
        DraftDetail second = staleDraft("d-2", "owner-b");
        when(draftRepository.findActiveDraftsOlderThan(80L)).thenReturn(List.of(first, second));

        int purged = draftService.purgeActiveOlderThan(80L);

        assertThat(purged).isEqualTo(2);
        verify(draftRepository, times(2)).updateStatus(payloadCaptor.capture());
        assertThat(payloadCaptor.getAllValues())
                .extracting(captured -> captured.getDraft().getStatus())
                .containsOnly(DraftConstants.STATUS_DISCARDED);
        assertThat(payloadCaptor.getAllValues())
                .extracting(captured -> captured.getDraft().getLastModifiedBy())
                .containsExactly("owner-a", "owner-b");
    }

    @Test
    void purgeByStatusOlderThan_staleDrafts_deletesEachAndReturnsSize() {
        DraftDetail stale = staleDraft("d-3", "owner-c");
        when(draftRepository.findSubmittedOrDiscardedOlderThan(DraftConstants.STATUS_SUBMITTED, 90L))
                .thenReturn(List.of(stale));

        int purged = draftService.purgeByStatusOlderThan(DraftConstants.STATUS_SUBMITTED, 90L);

        assertThat(purged).isEqualTo(1);
        verify(draftRepository).delete(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getDraft()).isSameAs(stale);
        assertThat(payloadCaptor.getValue().getDraft().getDraftId()).isEqualTo("d-3");
        verify(draftRepository, never()).updateStatus(any());
    }

    @Test
    void reconcileOrphanedDrafts_candidatesPresent_submitsEachAndReturnsSize() {
        DraftDetail orphan = staleDraft("d-4", "owner-d");
        when(draftRepository.findActiveDraftsWithModuleEntity()).thenReturn(List.of(orphan));

        int reconciled = draftService.reconcileOrphanedDrafts();

        assertThat(reconciled).isEqualTo(1);
        verify(draftRepository).updateStatus(payloadCaptor.capture());
        DraftDetail sent = payloadCaptor.getValue().getDraft();
        assertThat(sent.getStatus()).isEqualTo(DraftConstants.STATUS_SUBMITTED);
        assertThat(sent.getLastModifiedBy()).isEqualTo("owner-d");
        assertThat(sent.getLastModifiedTime()).isPositive();
        assertThat(payloadCaptor.getValue().getRequestInfo()).isNull();
    }

    @Test
    void reconcileOrphanedDrafts_noCandidates_returnsZero() {
        when(draftRepository.findActiveDraftsWithModuleEntity()).thenReturn(List.of());

        assertThat(draftService.reconcileOrphanedDrafts()).isZero();
        verify(draftRepository, never()).updateStatus(any());
    }

    private static RequestInfo caller(String uuid, String userType) {
        return RequestInfo.builder()
                .apiId("draft")
                .ver("1.0")
                .msgId("m-1")
                .userInfo(User.builder().uuid(uuid).type(userType).build())
                .build();
    }

    private static DraftDetail savableDraft() {
        return DraftDetail.builder()
                .tenantId("pb.amritsar")
                .businessService("PT")
                .draftData(Map.<String, Object>of("step", 1))
                .build();
    }

    private static DraftDetail staleDraft(String draftId, String createdBy) {
        return DraftDetail.builder()
                .draftId(draftId)
                .createdBy(createdBy)
                .tenantId("pb.amritsar")
                .build();
    }

    private static DraftRequest saveRequest(RequestInfo requestInfo, DraftDetail draft) {
        return DraftRequest.builder()
                .requestInfo(requestInfo)
                .draft(draft)
                .build();
    }
}
