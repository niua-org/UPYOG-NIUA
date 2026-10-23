// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.cdwm.config.CNDConfiguration;
import org.upyog.cdwm.repository.impl.CNDServiceRepositoryImpl;
import org.upyog.cdwm.service.CalculationService;
import org.upyog.cdwm.service.EnrichmentService;
import org.upyog.cdwm.service.UserService;
import org.upyog.cdwm.service.WorkflowService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CNDServiceImplTest {
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private CalculationService calculationService;
    @Mock
    private CNDServiceRepositoryImpl cndApplicationRepository;
    @Mock
    private UserService userService;
    @Mock
    private CNDConfiguration config;

    private CNDServiceImpl subject;

    @BeforeEach
    void setUp() {
        subject = new CNDServiceImpl(enrichmentService, workflowService, calculationService, cndApplicationRepository, userService, config);
    }


    @Test
    void createConstructionAndDemolitionRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
