package org.egov.waterconnection.consumer;

import org.egov.waterconnection.service.PaymentUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReceiptConsumerTest {

    @Mock private PaymentUpdateService paymentUpdateService;

    @InjectMocks
    private ReceiptConsumer receiptConsumer;

    @BeforeEach
    void setStateTenant() {
        ReflectionTestUtils.setField(receiptConsumer, "stateLevelTenantID", "pb");
    }

    @Test
    void listenPayments_whenRecordReceived_delegatesToPaymentUpdateService() {
        HashMap<String, Object> record = new HashMap<>();
        record.put("paymentId", "pay-1");

        receiptConsumer.listenPayments(record);

        verify(paymentUpdateService).process(record);
    }
}
