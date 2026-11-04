package org.egov.waterconnection.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.waterconnection.service.DiffService;
import org.egov.waterconnection.service.WaterServiceImpl;
import org.egov.waterconnection.util.EncryptionDecryptionUtil;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditWorkFlowNotificationConsumerTest {

    @Mock private ObjectMapper mapper;
    @Mock private WaterServiceImpl waterServiceImpl;
    @Mock private DiffService diffService;
    @Mock private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @InjectMocks
    private EditWorkFlowNotificationConsumer editWorkFlowNotificationConsumer;

    @Test
    void listen_whenSearchReturnsConnection_triggersDiffNotification() {
        WaterConnection stored = new WaterConnection();
        stored.setApplicationNo("WS-EDIT-1");
        stored.setTenantId("pb.amritsar");
        WaterConnection incoming = new WaterConnection();
        incoming.setApplicationNo("WS-EDIT-1");
        incoming.setTenantId("pb.amritsar");
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .waterConnection(incoming)
                .build();
        when(mapper.convertValue(any(), eq(WaterConnectionRequest.class))).thenReturn(request);
        when(waterServiceImpl.search(any(), any())).thenReturn(List.of(stored));

        editWorkFlowNotificationConsumer.listen(new HashMap<>(), "ws-edit");

        verify(diffService).checkDifferenceAndSendEditNotification(request);
    }
}
