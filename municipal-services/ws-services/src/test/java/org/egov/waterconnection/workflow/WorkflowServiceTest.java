package org.egov.waterconnection.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.repository.ServiceRequestRepository;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.workflow.BusinessService;
import org.egov.waterconnection.web.models.workflow.BusinessServiceResponse;
import org.egov.waterconnection.web.models.workflow.ProcessInstance;
import org.egov.waterconnection.web.models.workflow.ProcessInstanceResponse;
import org.egov.waterconnection.web.models.workflow.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock private WSConfiguration config;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private ObjectMapper mapper;

    @InjectMocks
    private WorkflowService workflowService;

    private BusinessService businessService;

    @BeforeEach
    void businessServiceWithStates() {
        businessService = BusinessService.builder()
                .states(List.of(
                        State.builder().applicationStatus("INPROGRESS").isStateUpdatable(true).sla(5L).build(),
                        State.builder().applicationStatus("APPROVED").isStateUpdatable(false).sla(10L).build()))
                .build();
    }

    @Test
    void isStateUpdatable_whenStatusMatches_returnsConfiguredFlag() {
        assertThat(workflowService.isStateUpdatable("INPROGRESS", businessService)).isTrue();
        assertThat(workflowService.isStateUpdatable("APPROVED", businessService)).isFalse();
    }

    @Test
    void isStateUpdatable_whenStatusUnknown_returnsNull() {
        assertThat(workflowService.isStateUpdatable("UNKNOWN", businessService)).isNull();
    }

    @Test
    void getSlaForState_whenStatusMatches_returnsSlaAsBigDecimal() {
        when(config.getWfHost()).thenReturn("http://wf");
        when(config.getWfBusinessServiceSearchPath()).thenReturn("/business");
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(new Object());
        BusinessServiceResponse response = new BusinessServiceResponse();
        response.setBusinessServices(List.of(businessService));
        when(mapper.convertValue(any(), eq(BusinessServiceResponse.class))).thenReturn(response);

        BigDecimal sla = workflowService.getSlaForState("pb.amritsar", RequestInfo.builder().build(), "APPROVED",
                "WS");

        assertThat(sla).isEqualByComparingTo(BigDecimal.valueOf(10L));
    }

    @Test
    void validateInProgressWF_whenOpenWorkflowExists_throwsApplicationInWorkflow() {
        when(config.getWfHost()).thenReturn("http://wf");
        when(config.getWfProcessSearchPath()).thenReturn("/process");
        when(config.getModifyWSBusinessServiceName()).thenReturn("WS.MODIFY");
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(new Object());
        ProcessInstance openInstance = ProcessInstance.builder()
                .state(State.builder().isTerminateState(false).build())
                .build();
        ProcessInstanceResponse processInstanceResponse = new ProcessInstanceResponse();
        processInstanceResponse.setProcessInstances(List.of(openInstance));
        when(mapper.convertValue(any(), eq(ProcessInstanceResponse.class))).thenReturn(processInstanceResponse);
        WaterConnection connection = new WaterConnection();
        connection.setApplicationNo("WS-900");

        assertThatThrownBy(() -> workflowService.validateInProgressWF(List.of(connection), RequestInfo.builder().build(),
                "pb.amritsar"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Application already exist in WorkFlow");
    }

    @Test
    void getBusinessService_whenMapperFails_throwsParsingError() {
        when(config.getWfHost()).thenReturn("http://wf");
        when(config.getWfBusinessServiceSearchPath()).thenReturn("/business");
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(new Object());
        when(mapper.convertValue(any(), eq(BusinessServiceResponse.class)))
                .thenThrow(new IllegalArgumentException("bad json"));

        assertThatThrownBy(() -> workflowService.getBusinessService("pb.amritsar", RequestInfo.builder().build(), "WS"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Failed to parse response of calculate");
    }
}
