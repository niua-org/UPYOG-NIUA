package org.egov.pt.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.workflow.Action;
import org.egov.pt.models.workflow.BusinessService;
import org.egov.pt.models.workflow.BusinessServiceResponse;
import org.egov.pt.models.workflow.ProcessInstance;
import org.egov.pt.models.workflow.ProcessInstanceRequest;
import org.egov.pt.models.workflow.ProcessInstanceResponse;
import org.egov.pt.models.workflow.State;
import org.egov.pt.repository.ServiceRequestRepository;
import org.egov.pt.util.PropertyUtil;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock private PropertyConfiguration configs;
    @Mock private ServiceRequestRepository restRepo;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper mapper;
    @Mock private PropertyUtil utils;
    @Mock private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private WorkflowService workflowService;

    @Test
    void isStateUpdatable_whenStateMatches_returnsFlagFromBusinessService() {
        State state = State.builder().state("APPROVED").isStateUpdatable(true).build();
        BusinessService businessService = BusinessService.builder().states(List.of(state)).build();

        assertThat(workflowService.isStateUpdatable("APPROVED", businessService)).isTrue();
    }

    @Test
    void isStateUpdatable_whenStateUnknown_returnsNull() {
        BusinessService businessService = BusinessService.builder().states(List.of()).build();

        assertThat(workflowService.isStateUpdatable("UNKNOWN", businessService)).isNull();
    }

    @Test
    void getBusinessService_whenNoServicesFound_throwsBusinessServiceNotFound() {
        when(configs.getWfHost()).thenReturn("http://wf");
        when(configs.getWfBusinessServiceSearchPath()).thenReturn("/search");
        when(restRepo.fetchResult(any(), any())).thenReturn(Optional.of(new Object()));
        BusinessServiceResponse empty = new BusinessServiceResponse();
        empty.setBusinessServices(List.of());
        when(mapper.convertValue(any(), eq(BusinessServiceResponse.class))).thenReturn(empty);

        assertThatThrownBy(() -> workflowService.getBusinessService("pb.amritsar", "PT", RequestInfo.builder().build()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("businessService PT is not found");
    }

    @Test
    void callWorkFlow_whenTransitionSucceeds_returnsStateFromResponse() {
        State state = State.builder().state("OPEN").applicationStatus("ACTIVE").build();
        ProcessInstance pi = ProcessInstance.builder().state(state).build();
        ProcessInstanceResponse response = new ProcessInstanceResponse();
        response.setProcessInstances(List.of(pi));
        when(configs.getWfHost()).thenReturn("http://wf");
        when(configs.getWfTransitionPath()).thenReturn("/transition");
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(Optional.of(new Object()));
        when(mapper.convertValue(any(), eq(ProcessInstanceResponse.class))).thenReturn(response);

        State result = workflowService.callWorkFlow(ProcessInstanceRequest.builder().build());

        assertThat(result.getState()).isEqualTo("OPEN");
    }
}
