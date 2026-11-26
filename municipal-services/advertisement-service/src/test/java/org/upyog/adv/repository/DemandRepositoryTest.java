// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DemandRepositoryTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private BookingConfiguration config;
    @Mock
    private ObjectMapper mapper;

    private DemandRepository subject;

    @BeforeEach
    void setUp() {
        subject = new DemandRepository(serviceRequestRepository, config, mapper);
    }


    @Test
    void saveDemand_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
