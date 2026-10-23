// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.repository.impl.BookingRepositoryImpl;
import org.upyog.adv.service.BookingService;
import org.upyog.adv.service.PaymentTimerService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private BookingConfiguration configs;
    @Mock
    private BookingService bookingService;
    @Mock
    private BookingRepositoryImpl bookingRepo;
    @Mock
    private PaymentTimerService paymentTimerService;

    private PaymentService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentService(mapper, configs, bookingService, bookingRepo, paymentTimerService);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
