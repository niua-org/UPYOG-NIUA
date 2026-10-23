// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.service.WorkflowService;
import org.egov.pt.util.AssessmentUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssessmentEnrichmentServiceTest {
    @Mock
    private AssessmentUtils assessmentUtils;
    @Mock
    private PropertyConfiguration config;
    @Mock
    private WorkflowService workflowService;

    private AssessmentEnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new AssessmentEnrichmentService(assessmentUtils, config, workflowService);
    }


    @Test
    void enrichAssessmentCreate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
