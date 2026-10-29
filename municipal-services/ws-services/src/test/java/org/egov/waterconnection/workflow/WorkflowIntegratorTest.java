package org.egov.waterconnection.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.egov.waterconnection.web.models.Property;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.web.models.workflow.ProcessInstance;
import org.egov.waterconnection.web.models.workflow.ProcessInstanceResponse;
import org.egov.waterconnection.web.models.workflow.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowIntegratorTest {

    @Mock private WSConfiguration config;
    @Mock private ObjectMapper mapper;
    @Mock private RestTemplate rest;
    @Mock private WaterServicesUtil wsUtil;

    @InjectMocks
    private WorkflowIntegrator workflowIntegrator;

    @Test
    void callWorkFlow_whenTransitionSucceeds_updatesApplicationStatus() {
        when(config.getBusinessServiceValue()).thenReturn("WS");
        when(config.getWfHost()).thenReturn("http://wf");
        when(config.getWfTransitionPath()).thenReturn("/transition");
        when(wsUtil.isModifyConnectionRequest(any())).thenReturn(false);
        ProcessInstance processInstance = ProcessInstance.builder().action(WCConstants.ACTION_INITIATE).build();
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setApplicationNo("WS-APP-1");
        waterConnection.setApplicationStatus(WCConstants.STATUS_INITIATED);
        waterConnection.setApplicationType(WCConstants.NEW_WATER_CONNECTION);
        waterConnection.setProcessInstance(processInstance);
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .waterConnection(waterConnection)
                .build();
        Property property = Property.builder().tenantId("pb.amritsar").build();
        State nextState = State.builder().applicationStatus("PENDING_FOR_FIELD_INSPECTION").build();
        ProcessInstanceResponse response = new ProcessInstanceResponse();
        response.setProcessInstances(List.of(ProcessInstance.builder()
                .businessId("WS-APP-1")
                .state(nextState)
                .build()));
        when(rest.postForObject(anyString(), any(), eq(Map.class))).thenReturn(Map.of());
        when(mapper.convertValue(any(), eq(ProcessInstanceResponse.class))).thenReturn(response);

        workflowIntegrator.callWorkFlow(request, property);

        assertThat(waterConnection.getApplicationStatus()).isEqualTo("PENDING_FOR_FIELD_INSPECTION");
    }
}
