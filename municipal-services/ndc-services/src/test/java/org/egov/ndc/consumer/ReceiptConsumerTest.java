// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ndc.service.PaymentUpdateService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReceiptConsumerTest {
    @Mock
    private PaymentUpdateService paymentUpdateService;

    private ReceiptConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new ReceiptConsumer(paymentUpdateService);
    }


    @Test
    void listenPayments_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
