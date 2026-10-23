// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.repository.CommunityHallBookingRepository;
import org.upyog.chb.service.BookingTimerService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BookingTimerServiceTest {
    @Mock
    private CommunityHallBookingRepository bookingRepository;
    @Mock
    private CommunityHallBookingConfiguration bookingConfiguration;
    @Mock
    private ObjectProvider<PaymentTimerRedisService> paymentTimerRedis;
    @Mock
    private BookingTimerService self;

    private BookingTimerService subject;

    @BeforeEach
    void setUp() {
        subject = new BookingTimerService(bookingRepository, bookingConfiguration, paymentTimerRedis, self);
    }


    @Test
    void managePaymentTimer_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
