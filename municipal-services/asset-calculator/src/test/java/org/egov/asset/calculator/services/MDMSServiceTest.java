// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.calculator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.calculator.config.CalculatorConfig;
import org.egov.asset.calculator.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MDMSServiceTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private CalculatorConfig config;

    private MDMSService subject;

    @BeforeEach
    void setUp() {
        subject = new MDMSService(serviceRequestRepository, config);
    }


    @Test
    void mDMSCall_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
