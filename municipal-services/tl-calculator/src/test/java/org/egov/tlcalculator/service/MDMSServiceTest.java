// generated-by: generate_per_class_unit_tests.py
package org.egov.tlcalculator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tlcalculator.config.TLCalculatorConfigs;
import org.egov.tlcalculator.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MDMSServiceTest {
    @Mock
    private TLCalculatorConfigs config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private MDMSService subject;

    @BeforeEach
    void setUp() {
        subject = new MDMSService(config, serviceRequestRepository);
    }


    @Test
    void getTaxPeriods_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
