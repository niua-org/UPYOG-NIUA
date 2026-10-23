// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ChallanConfiguration config;

    private CalculationService subject;

    @BeforeEach
    void setUp() {
        subject = new CalculationService(serviceRequestRepository, mapper, config);
    }


    @Test
    void addCalculation_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
