package org.egov.pt.service;

import org.egov.pt.config.PropertyConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {

    @Mock private PropertyConfiguration propertyConfiguration;
    @Mock private org.egov.pt.repository.PropertyRepository propertyRepository;
    @Mock private org.egov.pt.util.NotificationUtil util;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper mapper;
    @Mock private org.springframework.web.client.RestTemplate restTemplate;

    @InjectMocks
    private PaymentNotificationService paymentNotificationService;

    @Test
    void process_whenUnknownTopic_doesNothing() {
        when(propertyConfiguration.getReceiptTopic()).thenReturn("receipt-topic");
        when(propertyConfiguration.getPgTopic()).thenReturn("pg-topic");

        paymentNotificationService.process(new HashMap<>(), "other-topic");

        verify(mapper, never()).convertValue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(org.egov.pt.models.transaction.TransactionRequest.class));
    }
}
