// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.repository.BookingRepository;
import org.upyog.adv.repository.IdGenRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private BookingConfiguration config;
    @Mock
    private IdGenRepository idGenRepository;
    @Mock
    private BookingRepository bookingRepository;

    private EnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new EnrichmentService(config, idGenRepository, bookingRepository);
    }


    @Test
    void enrichCreateBookingRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
