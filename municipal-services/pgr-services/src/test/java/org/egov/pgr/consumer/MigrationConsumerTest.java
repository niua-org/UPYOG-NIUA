package org.egov.pgr.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pgr.service.MigrationService;
import org.egov.pgr.web.models.pgrV1.Service;
import org.egov.pgr.web.models.pgrV1.ServiceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationConsumerTest {

    @Mock private MigrationService migrationService;
    @Mock private ObjectMapper mapper;

    @InjectMocks
    private MigrationConsumer migrationConsumer;

    @Test
    void listen_whenRecordValid_invokesMigrationService() throws Exception {
        ServiceResponse serviceResponse = new ServiceResponse();
        serviceResponse.setServices(List.of(Service.builder().tenantId("pg.citya").build()));
        when(mapper.convertValue(any(), eq(ServiceResponse.class))).thenReturn(serviceResponse);

        migrationConsumer.listen(new HashMap<>(), "pgr-migration");

        verify(migrationService).migrate(serviceResponse);
    }

    @Test
    void listen_whenMigrationFails_doesNotPropagateException() throws Exception {
        ServiceResponse serviceResponse = new ServiceResponse();
        serviceResponse.setServices(List.of(Service.builder().tenantId("pg.citya").build()));
        when(mapper.convertValue(any(), eq(ServiceResponse.class))).thenReturn(serviceResponse);
        doThrow(new RuntimeException("migrate failed")).when(migrationService).migrate(any());

        migrationConsumer.listen(new HashMap<>(), "pgr-migration");
    }
}
