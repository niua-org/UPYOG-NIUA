// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.sv.config.StreetVendingConfiguration;
import org.upyog.sv.repository.DemandRepository;
import org.upyog.sv.service.CalculationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {
    @Mock
    private StreetVendingConfiguration config;
    @Mock
    private CalculationService calculationService;
    @Mock
    private DemandRepository demandRepository;

    private DemandService subject;

    @BeforeEach
    void setUp() {
        subject = new DemandService(config, calculationService, demandRepository);
    }


    @Test
    void createDemand_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
