// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.sv.util.MdmsUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {
    @Mock
    private MdmsUtil mdmsUtil;

    private CalculationService subject;

    @BeforeEach
    void setUp() {
        subject = new CalculationService(mdmsUtil);
    }


    @Test
    void calculateDemand_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
