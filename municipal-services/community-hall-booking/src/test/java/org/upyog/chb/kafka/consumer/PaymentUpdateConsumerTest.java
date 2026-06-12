package org.upyog.chb.kafka.consumer;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.upyog.chb.service.PaymentNotificationService;

class PaymentUpdateConsumerTest {

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @InjectMocks
    private PaymentUpdateConsumer paymentUpdateConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testPaymentSuccess() throws Exception {
        // Arrange
        HashMap<String, Object> record = new HashMap<>();
        record.put("key", "value");
        String topic = "receipt-create-topic";

        // Act
        paymentUpdateConsumer.paymentSuccess(record, topic);

        // Assert
        verify(paymentNotificationService).process(record, topic);
    }

    @Test
    void testPaymentUpdate() {
        // Arrange
        HashMap<String, Object> record = new HashMap<>();
        record.put("key", "value");
        String topic = "update-pg-txns-topic";

        // Act
        paymentUpdateConsumer.paymentUpdate(record, topic);

        // Assert
        verify(paymentNotificationService).processTransaction(record, topic, null);
    }

    @Test
    void testPaymentSuccessWithException() throws Exception {
        // Arrange
        HashMap<String, Object> record = new HashMap<>();
        record.put("key", "value");
        String topic = "receipt-create-topic";

        doThrow(new RuntimeException("Processing error")).when(paymentNotificationService).process(record, topic);


        // Act & Assert
        assertThrows(RuntimeException.class, () -> paymentUpdateConsumer.paymentSuccess(record, topic));

        // Assert
        verify(paymentNotificationService).process(record, topic);
    }
}