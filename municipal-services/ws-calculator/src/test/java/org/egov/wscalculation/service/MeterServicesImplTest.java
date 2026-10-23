// generated-by: generate_per_class_unit_tests.py
package org.egov.wscalculation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.wscalculation.service.EnrichmentService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MeterServicesImplTest {
    @Mock
    private EnrichmentService enrichmentService;

    private MeterServicesImpl subject;

    @BeforeEach
    void setUp() {
        subject = new MeterServicesImpl(enrichmentService);
    }


    @Test
    void createMeterReading_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
