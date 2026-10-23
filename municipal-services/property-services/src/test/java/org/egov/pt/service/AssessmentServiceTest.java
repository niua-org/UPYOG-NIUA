// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.producer.PropertyProducer;
import org.egov.pt.repository.AssessmentRepository;
import org.egov.pt.service.AssessmentEnrichmentService;
import org.egov.pt.service.CalculationService;
import org.egov.pt.service.DiffService;
import org.egov.pt.service.WorkflowService;
import org.egov.pt.util.AssessmentUtils;
import org.egov.pt.validator.AssessmentValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {
    @Mock
    private AssessmentValidator validator;
    @Mock
    private PropertyProducer producer;
    @Mock
    private PropertyConfiguration props;
    @Mock
    private AssessmentRepository repository;
    @Mock
    private AssessmentEnrichmentService assessmentEnrichmentService;
    @Mock
    private PropertyConfiguration config;
    @Mock
    private DiffService diffService;
    @Mock
    private AssessmentUtils utils;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private CalculationService calculationService;

    private AssessmentService subject;

    @BeforeEach
    void setUp() {
        subject = new AssessmentService(validator, producer, props, repository, assessmentEnrichmentService, config, diffService, utils, workflowService, calculationService);
    }


    @Test
    void createAssessment_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
