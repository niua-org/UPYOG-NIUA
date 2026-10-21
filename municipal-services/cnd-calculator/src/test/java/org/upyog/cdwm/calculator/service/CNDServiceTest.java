// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.calculator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.cdwm.calculator.config.CalculatorConfig;
import org.upyog.cdwm.calculator.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CNDServiceTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private CalculatorConfig config;

    private CNDService subject;

    @BeforeEach
    void setUp() {
        subject = new CNDService(serviceRequestRepository, mapper, config);
    }


    @Test
    void getCNDApplication_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
