package org.egov.pt.consumer;

import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.service.PaymentNotificationService;
import org.egov.pt.service.PaymentUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptConsumerTest {

    @Mock private PaymentUpdateService paymentUpdateService;
    @Mock private PaymentNotificationService paymentNotificationService;
    @Mock private PropertyConfiguration config;

    @InjectMocks
    private ReceiptConsumer receiptConsumer;

    @BeforeEach
    void setStateTenant() {
        ReflectionTestUtils.setField(receiptConsumer, "stateLevelTenantID", "pb");
    }

    @Test
    void listenPayments_whenTopicMatchesPattern_processesPaymentAndNotification() {
        HashMap<String, Object> record = new HashMap<>();
        when(config.getReceiptTopicPattern()).thenReturn(".*receipt.*");
        when(config.getReceiptTopic()).thenReturn("receipt-topic");

        receiptConsumer.listenPayments(record, "pb.receipt.create");

        verify(paymentUpdateService).process(record);
        verify(paymentNotificationService).process(record, "receipt-topic");
    }
}
