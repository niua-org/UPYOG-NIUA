// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.calculator.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.calculator.config.CalculatorConfig;
import org.egov.asset.calculator.services.ProcessDepreciationV2;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DepreciationSchedulerTest {
    @Mock
    private ProcessDepreciationV2 processDepreciation;
    @Mock
    private CalculatorConfig config;

    private DepreciationScheduler subject;

    @BeforeEach
    void setUp() {
        subject = new DepreciationScheduler(processDepreciation, config);
    }


    @Test
    void scheduleBulkDepreciation_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
