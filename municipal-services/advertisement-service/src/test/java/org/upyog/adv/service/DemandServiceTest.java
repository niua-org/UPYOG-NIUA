// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.repository.DemandRepository;
import org.upyog.adv.service.CalculationService;
import org.upyog.adv.util.MdmsUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {
    @Mock
    private BookingConfiguration config;
    @Mock
    private CalculationService calculationService;
    @Mock
    private DemandRepository demandRepository;
    @Mock
    private MdmsUtil mdmsUtil;

    private DemandService subject;

    @BeforeEach
    void setUp() {
        subject = new DemandService(config, calculationService, demandRepository, mdmsUtil);
    }


    @Test
    void createDemand_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
