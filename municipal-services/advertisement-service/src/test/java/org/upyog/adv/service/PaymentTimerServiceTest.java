// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.upyog.adv.repository.BookingRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentTimerServiceTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ObjectProvider<PaymentTimerRedisService> paymentTimerRedis;

    private PaymentTimerService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentTimerService(bookingRepository, paymentTimerRedis);
    }


    @Test
    void insertBookingIdForTimer_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
