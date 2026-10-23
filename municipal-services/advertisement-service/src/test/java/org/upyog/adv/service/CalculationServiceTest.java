// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.util.MdmsUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {
    @Mock
    private MdmsUtil mdmsUtil;
    @Mock
    private BookingConfiguration config;

    private CalculationService subject;

    @BeforeEach
    void setUp() {
        subject = new CalculationService(mdmsUtil, config);
    }


    @Test
    void calculateDemand_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
