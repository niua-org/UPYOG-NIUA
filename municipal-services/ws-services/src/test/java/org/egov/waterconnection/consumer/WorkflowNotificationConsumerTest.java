package org.egov.waterconnection.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.service.WaterServiceImpl;
import org.egov.waterconnection.service.WorkflowNotificationService;
import org.egov.waterconnection.util.EncryptionDecryptionUtil;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.web.models.workflow.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowNotificationConsumerTest {

    @Mock private WorkflowNotificationService workflowNotificationService;
    @Mock private ObjectMapper mapper;
    @Mock private EncryptionDecryptionUtil encryptionDecryptionUtil;
    @Mock private WaterServiceImpl waterServiceImpl;

    @InjectMocks
    private WorkflowNotificationConsumer workflowNotificationConsumer;

    @Test
    void listen_whenNotificationDisabledForStatus_skipsProcessing() {
        ProcessInstance processInstance = ProcessInstance.builder().action("DISABLED_ACTION").build();
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setTenantId("pb.amritsar");
        waterConnection.setApplicationStatus("UNKNOWN_STATUS");
        waterConnection.setProcessInstance(processInstance);
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().roles(List.of()).build()).build())
                .waterConnection(waterConnection)
                .build();
        when(mapper.convertValue(any(), eq(WaterConnectionRequest.class))).thenReturn(request);

        workflowNotificationConsumer.listen(new HashMap<>(), "ws-update");

        verify(workflowNotificationService, never()).process(any(), anyString());
    }

    @Test
    void listen_whenEnabledStatusAndEmployeeRole_processesNotification() {
        ProcessInstance processInstance = ProcessInstance.builder().action(WCConstants.ACTION_INITIATE).build();
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setTenantId("pb.amritsar");
        waterConnection.setApplicationStatus(WCConstants.STATUS_INITIATED);
        waterConnection.setProcessInstance(processInstance);
        User employee = User.builder().roles(List.of(Role.builder().code("EMPLOYEE").build())).build();
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(employee).build())
                .waterConnection(waterConnection)
                .build();
        when(mapper.convertValue(any(), eq(WaterConnectionRequest.class))).thenReturn(request);
        when(encryptionDecryptionUtil.decryptObject(any(), anyString(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        workflowNotificationConsumer.listen(new HashMap<>(), "ws-update");

        verify(workflowNotificationService).process(request, "ws-update");
    }
}
