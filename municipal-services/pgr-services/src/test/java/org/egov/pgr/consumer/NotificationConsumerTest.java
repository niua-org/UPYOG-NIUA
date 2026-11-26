package org.egov.pgr.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pgr.service.NotificationService;
import org.egov.pgr.web.models.Address;
import org.egov.pgr.web.models.Service;
import org.egov.pgr.web.models.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock private NotificationService notificationService;
    @Mock private ObjectMapper mapper;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void listen_whenRecordValid_delegatesToNotificationService() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .service(Service.builder().tenantId("pg.citya").address(Address.builder().build()).build())
                .build();
        when(mapper.convertValue(any(), eq(ServiceRequest.class))).thenReturn(request);

        notificationConsumer.listen(new HashMap<>(), "pg.pgr.notification");

        verify(notificationService).process(request, "pg.pgr.notification");
    }

    @Test
    void listen_whenProcessingFails_doesNotPropagateException() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .service(Service.builder().tenantId("pg.citya").address(Address.builder().build()).build())
                .build();
        when(mapper.convertValue(any(), eq(ServiceRequest.class))).thenReturn(request);
        doThrow(new RuntimeException("downstream")).when(notificationService).process(any(), any());

        notificationConsumer.listen(new HashMap<>(), "pg.pgr.notification");
    }
}
