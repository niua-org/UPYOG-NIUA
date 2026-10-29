package org.egov.bpa.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.bpa.service.notification.BPANotificationService;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BPAConsumerTest {

    @Mock
    private BPANotificationService notificationService;

    @InjectMocks
    private BPAConsumer consumer;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void listen_whenValidRecord_processesNotification() throws Exception {
        BPARequest request = BPARequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .BPA(BPA.builder().tenantId("pb.amritsar").applicationNo("BPA-001").build())
                .build();
        HashMap<String, Object> record = mapper.convertValue(request, new TypeReference<>() {});

        consumer.listen(record, "persister.update.buildingplan.topic");

        verify(notificationService).process(any(BPARequest.class));
    }
}
