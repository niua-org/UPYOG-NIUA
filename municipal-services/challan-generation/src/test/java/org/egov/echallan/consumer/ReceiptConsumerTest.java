package org.egov.echallan.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallan.service.PaymentUpdateService;

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
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
