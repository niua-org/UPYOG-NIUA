// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.service.PaymentUpdateService;
import org.egov.tl.service.notification.PaymentNotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReceiptConsumerTest {
    @Mock
    private PaymentUpdateService paymentUpdateService;
    @Mock
    private PaymentNotificationService paymentNotificationService;

    private ReceiptConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new ReceiptConsumer(paymentUpdateService, paymentNotificationService);
    }


    @Test
    void listenPayments_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
