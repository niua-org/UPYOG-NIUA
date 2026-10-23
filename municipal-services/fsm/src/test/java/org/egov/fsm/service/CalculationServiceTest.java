// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.fsm.config.FSMConfiguration;
import org.egov.fsm.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private FSMConfiguration config;

    private CalculationService subject;

    @BeforeEach
    void setUp() {
        subject = new CalculationService(serviceRequestRepository, config);
    }


    @Test
    void addCalculation_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
