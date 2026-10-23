// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.repository.IdGenRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private CommunityHallBookingConfiguration config;
    @Mock
    private IdGenRepository idGenRepository;

    private EnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new EnrichmentService(config, idGenRepository);
    }


    @Test
    void enrichCreateBookingRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
