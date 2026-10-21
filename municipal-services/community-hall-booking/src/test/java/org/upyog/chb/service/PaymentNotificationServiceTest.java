// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.repository.CommunityHallBookingRepository;
import org.upyog.chb.repository.ServiceRequestRepository;
import org.upyog.chb.service.CommunityHallBookingService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private CommunityHallBookingConfiguration configs;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private CommunityHallBookingService bookingService;
    @Mock
    private CommunityHallBookingRepository bookingRepository;

    private PaymentNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentNotificationService(mapper, configs, serviceRequestRepository, bookingService, bookingRepository);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
