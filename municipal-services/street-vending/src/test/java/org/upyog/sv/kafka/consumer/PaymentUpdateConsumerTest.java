package org.upyog.sv.kafka.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.sv.service.PaymentService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentUpdateConsumerTest {
    @Mock
    private PaymentService paymentService;

    private PaymentUpdateConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentUpdateConsumer(paymentService);
    }


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
