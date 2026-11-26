package org.upyog.draft.web.controllers;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.upyog.draft.service.DraftService;
import org.upyog.draft.web.models.DraftDeleteRequest;
import org.upyog.draft.web.models.DraftDetail;
import org.upyog.draft.web.models.DraftMarkSubmittedRequest;
import org.upyog.draft.web.models.DraftRequest;
import org.upyog.draft.web.models.DraftResponse;
import org.upyog.draft.web.models.DraftSearchCriteria;
import org.upyog.draft.web.models.DraftSearchRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftControllerTest {

    @Mock
    private DraftService draftService;

    @InjectMocks
    private DraftController controller;

    @Test
    void save_delegatesToServiceAndReturnsOk() {
        DraftRequest request = draftRequest();
        DraftResponse response = DraftResponse.builder().draft(request.getDraft()).build();
        when(draftService.save(request)).thenReturn(response);

        ResponseEntity<DraftResponse> result = controller.save(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(draftService).save(same(request));
    }

    @Test
    void search_delegatesToServiceAndReturnsOk() {
        DraftSearchRequest request = DraftSearchRequest.builder()
                .requestInfo(requestInfo())
                .criteria(DraftSearchCriteria.builder().tenantId("pb.amritsar").build())
                .build();
        DraftResponse response = DraftResponse.builder().drafts(List.of()).build();
        when(draftService.search(request)).thenReturn(response);

        ResponseEntity<DraftResponse> result = controller.search(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void count_delegatesToServiceAndReturnsOk() {
        DraftSearchRequest request = DraftSearchRequest.builder()
                .requestInfo(requestInfo())
                .criteria(DraftSearchCriteria.builder().tenantId("pb.amritsar").build())
                .build();
        DraftResponse response = DraftResponse.builder().count(3).build();
        when(draftService.count(request)).thenReturn(response);

        ResponseEntity<DraftResponse> result = controller.count(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getCount()).isEqualTo(3);
    }

    @Test
    void delete_wrapsPayloadIntoDraftRequest() {
        DraftDetail draft = DraftDetail.builder().draftId("d-1").tenantId("pb.amritsar").build();
        DraftDeleteRequest deleteRequest = DraftDeleteRequest.builder()
                .requestInfo(requestInfo())
                .draft(draft)
                .build();
        DraftResponse response = DraftResponse.builder().draft(draft).build();
        when(draftService.delete(any())).thenReturn(response);

        ResponseEntity<DraftResponse> result = controller.delete(deleteRequest);

        ArgumentCaptor<DraftRequest> captor = ArgumentCaptor.forClass(DraftRequest.class);
        verify(draftService).delete(captor.capture());
        assertThat(captor.getValue().getRequestInfo()).isSameAs(deleteRequest.getRequestInfo());
        assertThat(captor.getValue().getDraft()).isSameAs(draft);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void markSubmitted_wrapsPayloadIntoDraftRequest() {
        DraftDetail draft = DraftDetail.builder().draftId("d-2").tenantId("pb.amritsar").build();
        DraftMarkSubmittedRequest submittedRequest = DraftMarkSubmittedRequest.builder()
                .requestInfo(requestInfo())
                .draft(draft)
                .build();
        when(draftService.markSubmitted(any())).thenReturn(DraftResponse.builder().draft(draft).build());

        controller.markSubmitted(submittedRequest);

        ArgumentCaptor<DraftRequest> captor = ArgumentCaptor.forClass(DraftRequest.class);
        verify(draftService).markSubmitted(captor.capture());
        assertThat(captor.getValue().getDraft()).isSameAs(draft);
    }

    private static RequestInfo requestInfo() {
        return RequestInfo.builder()
                .apiId("draft")
                .userInfo(User.builder().uuid("user-1").type("CITIZEN").build())
                .build();
    }

    private static DraftRequest draftRequest() {
        return DraftRequest.builder()
                .requestInfo(requestInfo())
                .draft(DraftDetail.builder()
                        .tenantId("pb.amritsar")
                        .businessService("PT")
                        .draftData(Map.of("step", 1))
                        .build())
                .build();
    }
}
