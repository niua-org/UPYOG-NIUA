// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.tp.config.TreePruningConfiguration;
import org.upyog.tp.repository.DemandRepository;
import org.upyog.tp.service.CalculationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {
    @Mock
    private CalculationService calculationService;
    @Mock
    private DemandRepository demandRepository;
    @Mock
    private TreePruningConfiguration config;

    private DemandService subject;

    @BeforeEach
    void setUp() {
        subject = new DemandService(calculationService, demandRepository, config);
    }


    @Test
    void createDemand_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
