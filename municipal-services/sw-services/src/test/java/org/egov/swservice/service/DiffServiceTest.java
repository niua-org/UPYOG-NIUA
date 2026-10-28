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
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .sewerageConnection(SewerageConnection.builder().applicationNo("SW-1").build())
                .build();
        doThrow(new RuntimeException("notify failed")).when(editNotificationService).sendEditNotification(request);

        diffService.checkDifferenceAndSendEditNotification(request, SewerageConnection.builder().build());

        verify(editNotificationService).sendEditNotification(request);
    }

    @Test
    void checkDifferenceAndSendEditNotification_whenSuccess_invokesNotification() {
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .sewerageConnection(SewerageConnection.builder().applicationNo("SW-2").build())
                .build();

        diffService.checkDifferenceAndSendEditNotification(request, SewerageConnection.builder().build());

        verify(editNotificationService).sendEditNotification(request);
    }
}
