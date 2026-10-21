// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.ndc.service.DemandService;
import org.egov.ndc.service.MDMSService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private MDMSService mdmsService;
    @Mock
    private DemandService demandService;

    private CalculationService subject;

    @BeforeEach
    void setUp() {
        subject = new CalculationService(mapper, mdmsService, demandService);
    }


    @Test
    void calculate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
