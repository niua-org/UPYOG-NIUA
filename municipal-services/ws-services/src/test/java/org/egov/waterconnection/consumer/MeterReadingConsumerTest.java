package org.egov.waterconnection.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.service.MeterReadingService;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingConsumerTest {

    @Mock private MeterReadingService meterReadingService;
    @Mock private ObjectMapper mapper;

    @InjectMocks
    private MeterReadingConsumer meterReadingConsumer;

    @Test
    void listen_whenMeteredConnection_processesMeterReading() {
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setTenantId("pb.amritsar");
        waterConnection.setConnectionType(WCConstants.METERED_CONNECTION);
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .waterConnection(waterConnection)
                .build();
        when(mapper.convertValue(any(), eq(WaterConnectionRequest.class))).thenReturn(request);

        meterReadingConsumer.listen(new HashMap<>(), "ws-meter");

        verify(meterReadingService).process(request, "ws-meter");
    }

    @Test
    void listen_whenNonMeteredConnection_skipsProcessing() {
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setTenantId("pb.amritsar");
        waterConnection.setConnectionType("Non Metered");
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .waterConnection(waterConnection)
                .build();
        when(mapper.convertValue(any(), eq(WaterConnectionRequest.class))).thenReturn(request);

        meterReadingConsumer.listen(new HashMap<>(), "ws-meter");

        verify(meterReadingService, never()).process(any(), anyString());
    }
}
