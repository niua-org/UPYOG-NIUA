// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private PropertyConfiguration config;

    private CalculationService subject;

    @BeforeEach
    void setUp() {
        subject = new CalculationService(mapper, serviceRequestRepository, config);
    }


    @Test
    void calculateTax_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
