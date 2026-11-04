package org.egov.tl.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tl.service.TradeLicenseService;
import org.egov.tl.service.notification.TLNotificationService;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.egov.tl.util.TLConstants.businessService_BPA;
import static org.egov.tl.util.TLConstants.businessService_TL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TradeLicenseConsumerTest {

    @Mock
    private TLNotificationService notificationService;
    @Mock
    private TradeLicenseService tradeLicenseService;

    private TradeLicenseConsumer consumer;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new TradeLicenseConsumer(notificationService, tradeLicenseService);
    }

    @Test
    void listen_whenTlLicense_processesNotification() throws Exception {
        TradeLicenseRequest request = tlRequest(businessService_TL);
        HashMap<String, Object> record = mapper.convertValue(request, new TypeReference<>() {});

        consumer.listen(record, "tl-notification");

        verify(notificationService).process(any(TradeLicenseRequest.class));
        verify(tradeLicenseService, org.mockito.Mockito.never()).checkEndStateAndAddBPARoles(any());
    }

    @Test
    void listen_whenBpaEndState_invokesRoleAssignmentAndNotification() throws Exception {
        TradeLicenseRequest request = tlRequest(businessService_BPA);
        HashMap<String, Object> record = mapper.convertValue(request, new TypeReference<>() {});

        consumer.listen(record, "tl-notification");

        verify(tradeLicenseService).checkEndStateAndAddBPARoles(any(TradeLicenseRequest.class));
        verify(notificationService).process(any(TradeLicenseRequest.class));
    }

    private static TradeLicenseRequest tlRequest(String businessService) {
        return TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(TradeLicense.builder()
                        .businessService(businessService)
                        .tenantId("pb.amritsar")
                        .build()))
                .build();
    }
}
