package org.egov.wf.web.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.wf.service.WorkflowService;
import org.egov.wf.util.ResponseInfoFactory;
import org.egov.wf.web.models.DashboardProcessInstanceResponse;
import org.egov.wf.web.models.ProcessInstance;
import org.egov.wf.web.models.ProcessInstanceRequest;
import org.egov.wf.web.models.ProcessInstanceResponse;
import org.egov.wf.web.models.ProcessInstanceSearchCriteria;
import org.egov.wf.web.models.RequestInfoWrapper;
import org.egov.wf.web.models.StatusCountRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @InjectMocks
    private WorkflowController workflowController;

    @Test
    void processTransitionReturnsInstancesAndResponseInfo() {
        RequestInfo requestInfo = new RequestInfo();
        ProcessInstance instance = ProcessInstance.builder().businessId("BIZ-1").build();
        ProcessInstanceRequest request = ProcessInstanceRequest.builder()
                .requestInfo(requestInfo)
                .processInstances(List.of(instance))
                .build();
        ResponseInfo responseInfo = new ResponseInfo();
        when(workflowService.transition(request)).thenReturn(List.of(instance));
        when(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true)).thenReturn(responseInfo);

        ResponseEntity<ProcessInstanceResponse> response = workflowController.processTransition(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProcessInstances()).containsExactly(instance);
        assertThat(response.getBody().getResponseInfo()).isSameAs(responseInfo);
        ArgumentCaptor<Boolean> successFlag = ArgumentCaptor.forClass(Boolean.class);
        verify(responseInfoFactory).createResponseInfoFromRequestInfo(eq(requestInfo), successFlag.capture());
        assertThat(successFlag.getValue()).isTrue();
    }

    @Test
    void processTransitionPropagatesARejectedTransition() {
        ProcessInstanceRequest request = ProcessInstanceRequest.builder()
                .requestInfo(new RequestInfo())
                .processInstances(Collections.emptyList())
                .build();
        when(workflowService.transition(request)).thenThrow(new CustomException("INVALID ACTION", "Action not found"));

        assertThatThrownBy(() -> workflowController.processTransition(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> assertThat(((CustomException) thrown).getCode()).isEqualTo("INVALID ACTION"));
        verifyNoInteractions(responseInfoFactory);
    }

    @Test
    void processTransitionCallsTheServiceBeforeFailingOnANullRequest() {
        when(workflowService.transition(null)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> workflowController.processTransition(null)).isInstanceOf(NullPointerException.class);

        verify(workflowService).transition(null);
        verifyNoInteractions(responseInfoFactory);
    }

    @Test
    void searchReturnsTheListAndInboxCountWithoutResponseInfo() {
        RequestInfo requestInfo = new RequestInfo();
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setTenantId("pb.amritsar");
        ProcessInstance instance = ProcessInstance.builder().businessId("BIZ-2").build();
        when(workflowService.search(requestInfo, criteria)).thenReturn(List.of(instance));
        when(workflowService.getUserBasedProcessInstancesCount(requestInfo, criteria)).thenReturn(4);

        ResponseEntity<ProcessInstanceResponse> response = workflowController.search(wrapper, criteria);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProcessInstances()).containsExactly(instance);
        assertThat(response.getBody().getTotalCount()).isEqualTo(4);
        assertThat(response.getBody().getResponseInfo()).isNull();
        verify(workflowService).search(requestInfo, criteria);
        verify(workflowService).getUserBasedProcessInstancesCount(requestInfo, criteria);
        verify(workflowService, never()).count(any(), any());
        verifyNoInteractions(responseInfoFactory);
    }

    @Test
    void searchAllowsAnEmptyResult() {
        RequestInfo requestInfo = new RequestInfo();
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        when(workflowService.search(requestInfo, criteria)).thenReturn(Collections.emptyList());
        when(workflowService.getUserBasedProcessInstancesCount(requestInfo, criteria)).thenReturn(0);

        ResponseEntity<ProcessInstanceResponse> response = workflowController.search(wrapper, criteria);

        assertThat(response.getBody().getProcessInstances()).isEmpty();
        assertThat(response.getBody().getTotalCount()).isZero();
    }

    @Test
    void searchPassesANullCriteriaThroughToTheService() {
        RequestInfo requestInfo = new RequestInfo();
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
        when(workflowService.search(requestInfo, null)).thenReturn(null);
        when(workflowService.getUserBasedProcessInstancesCount(requestInfo, null)).thenReturn(null);

        ResponseEntity<ProcessInstanceResponse> response = workflowController.search(wrapper, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProcessInstances()).isNull();
        assertThat(response.getBody().getTotalCount()).isNull();
    }

    @Test
    void searchRejectsANullRequestWrapper() {
        assertThatThrownBy(() -> workflowController.search(null, new ProcessInstanceSearchCriteria()))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(workflowService);
    }

    @Test
    void searchPropagatesAServiceFailure() {
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(new RequestInfo()).build();
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        when(workflowService.search(wrapper.getRequestInfo(), criteria))
                .thenThrow(new CustomException("SEARCH_ERR", "search failed"));

        assertThatThrownBy(() -> workflowController.search(wrapper, criteria))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> assertThat(((CustomException) thrown).getCode()).isEqualTo("SEARCH_ERR"));
        verify(workflowService, never()).getUserBasedProcessInstancesCount(any(), any());
    }

    @Test
    void countForcesTheNearingSlaFlagOff() {
        RequestInfo requestInfo = new RequestInfo();
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setIsNearingSlaCount(true);
        when(workflowService.count(requestInfo, criteria)).thenReturn(8);

        ResponseEntity<Integer> response = workflowController.count(wrapper, criteria);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(8);
        assertThat(criteria.getIsNearingSlaCount()).isFalse();
    }

    @Test
    void nearingSlaCountForcesTheFlagOn() {
        RequestInfo requestInfo = new RequestInfo();
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        when(workflowService.count(requestInfo, criteria)).thenReturn(2);

        ResponseEntity<Integer> response = workflowController.nearingSlaCount(wrapper, criteria);

        assertThat(response.getBody()).isEqualTo(2);
        assertThat(criteria.getIsNearingSlaCount()).isTrue();
    }

    @Test
    void statusCountPrefersCriteriaOnTheBody() {
        RequestInfo requestInfo = new RequestInfo();
        ProcessInstanceSearchCriteria bodyCriteria = new ProcessInstanceSearchCriteria();
        bodyCriteria.setBusinessService("PT");
        ProcessInstanceSearchCriteria queryCriteria = new ProcessInstanceSearchCriteria();
        queryCriteria.setTenantId("pb");
        StatusCountRequest request = new StatusCountRequest();
        request.setRequestInfo(requestInfo);
        request.setProcessInstanceSearchCriteria(bodyCriteria);
        when(workflowService.statusCount(requestInfo, bodyCriteria)).thenReturn(List.of("PENDING"));

        ResponseEntity<List> response = workflowController.StatusCount(request, queryCriteria);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("PENDING");
        verify(workflowService).statusCount(requestInfo, bodyCriteria);
    }

    @Test
    void statusCountFallsBackToTheQueryCriteria() {
        RequestInfo requestInfo = new RequestInfo();
        ProcessInstanceSearchCriteria queryCriteria = new ProcessInstanceSearchCriteria();
        StatusCountRequest request = new StatusCountRequest();
        request.setRequestInfo(requestInfo);
        when(workflowService.statusCount(requestInfo, queryCriteria)).thenReturn(Collections.emptyList());

        ResponseEntity<List> response = workflowController.StatusCount(request, queryCriteria);

        assertThat(response.getBody()).isEmpty();
        verify(workflowService).statusCount(requestInfo, queryCriteria);
    }

    @Test
    void escalatedSearchReturnsInstancesAndCount() {
        RequestInfo requestInfo = new RequestInfo();
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        ProcessInstance instance = ProcessInstance.builder().businessId("ESC-1").build();
        when(workflowService.escalatedApplicationsSearch(requestInfo, criteria)).thenReturn(List.of(instance));
        when(workflowService.countEscalatedApplications(requestInfo, criteria)).thenReturn(1);

        ResponseEntity<ProcessInstanceResponse> response = workflowController.searchEscalatedApplications(wrapper, criteria);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProcessInstances()).containsExactly(instance);
        assertThat(response.getBody().getTotalCount()).isEqualTo(1);
        assertThat(response.getBody().getResponseInfo()).isNull();
    }

    @Test
    void dashboardCountWrapsTheServiceCount() {
        RequestInfo requestInfo = new RequestInfo();
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        StatusCountRequest request = new StatusCountRequest();
        request.setRequestInfo(requestInfo);
        request.setProcessInstanceSearchCriteria(criteria);
        ResponseInfo responseInfo = new ResponseInfo();
        when(workflowService.getDashboardApplicationCount(requestInfo, criteria)).thenReturn(5);
        when(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true)).thenReturn(responseInfo);

        ResponseEntity<DashboardProcessInstanceResponse> response = workflowController.getApplicationCount(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalCount()).isEqualTo(5);
        assertThat(response.getBody().getResponseInfo()).isSameAs(responseInfo);
    }

    @Test
    void dashboardSearchSetsResponseInfoOnTheServicePayload() {
        RequestInfo requestInfo = new RequestInfo();
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        StatusCountRequest request = new StatusCountRequest();
        request.setRequestInfo(requestInfo);
        request.setProcessInstanceSearchCriteria(criteria);
        DashboardProcessInstanceResponse payload = DashboardProcessInstanceResponse.builder().totalCount(10).build();
        ResponseInfo responseInfo = new ResponseInfo();
        when(workflowService.getDashboardApplicationsWithCount(requestInfo, criteria)).thenReturn(payload);
        when(responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true)).thenReturn(responseInfo);

        ResponseEntity<DashboardProcessInstanceResponse> response = workflowController.getDashboardApplications(request);

        assertThat(response.getBody()).isSameAs(payload);
        assertThat(response.getBody().getResponseInfo()).isSameAs(responseInfo);
        assertThat(response.getBody().getTotalCount()).isEqualTo(10);
    }

    @Test
    void dashboardSearchFailsWhenTheServiceReturnsNull() {
        StatusCountRequest request = new StatusCountRequest();
        request.setRequestInfo(new RequestInfo());
        request.setProcessInstanceSearchCriteria(new ProcessInstanceSearchCriteria());
        when(workflowService.getDashboardApplicationsWithCount(any(), any())).thenReturn(null);

        assertThatThrownBy(() -> workflowController.getDashboardApplications(request))
                .isInstanceOf(NullPointerException.class);
    }
}
