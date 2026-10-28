package org.egov.swservice.service;

import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiffServiceTest {

    @Mock
    private EditNotificationService editNotificationService;

    @InjectMocks
    private DiffService diffService;

    @Test
    void checkDifferenceAndSendEditNotification_whenNotificationFails_doesNotPropagate() {
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setApplicationNo("SW-1");
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .sewerageConnection(connection)
                .build();
        doThrow(new RuntimeException("notify failed")).when(editNotificationService).sendEditNotification(request);

        diffService.checkDifferenceAndSendEditNotification(request, SewerageConnection.builder().build());

        verify(editNotificationService).sendEditNotification(request);
    }

    @Test
    void checkDifferenceAndSendEditNotification_whenSuccess_invokesNotification() {
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setApplicationNo("SW-2");
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .sewerageConnection(connection)
                .build();

        diffService.checkDifferenceAndSendEditNotification(request, SewerageConnection.builder().build());

        verify(editNotificationService).sendEditNotification(request);
    }
}
