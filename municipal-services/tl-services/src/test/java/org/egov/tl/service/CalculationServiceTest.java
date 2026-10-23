// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.util.TradeUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {
    @Mock
    private TradeUtil utils;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;

    private CalculationService subject;

    @BeforeEach
    void setUp() {
        subject = new CalculationService(utils, serviceRequestRepository, mapper);
    }


    @Test
    void addCalculation_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
