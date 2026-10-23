// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.repository.DemandRepository;
import org.upyog.chb.service.CalculationService;
import org.upyog.chb.util.MdmsUtil;
import org.upyog.chb.validator.CommunityHallBookingValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {
    @Mock
    private CommunityHallBookingConfiguration config;
    @Mock
    private CalculationService calculationService;
    @Mock
    private DemandRepository demandRepository;
    @Mock
    private CommunityHallBookingValidator bookingValidator;
    @Mock
    private MdmsUtil mdmsUtil;

    private DemandService subject;

    @BeforeEach
    void setUp() {
        subject = new DemandService(config, calculationService, demandRepository, bookingValidator, mdmsUtil);
    }


    @Test
    void createDemand_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
