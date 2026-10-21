// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ndc.config.NDCConfiguration;
import org.egov.ndc.repository.IdGenRepository;
import org.egov.ndc.workflow.WorkflowService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private NDCConfiguration config;
    @Mock
    private IdGenRepository idGenRepository;
    @Mock
    private WorkflowService workflowService;

    private EnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new EnrichmentService(config, idGenRepository, workflowService);
    }


    @Test
    void postStatusEnrichment_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
