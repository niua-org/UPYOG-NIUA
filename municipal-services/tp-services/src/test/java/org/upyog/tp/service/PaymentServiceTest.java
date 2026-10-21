// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.tp.config.TreePruningConfiguration;
import org.upyog.tp.service.WorkflowService;
import org.upyog.tp.service.impl.TreePruningServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private TreePruningConfiguration configs;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private TreePruningServiceImpl treePruningService;

    private PaymentService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentService(mapper, configs, workflowService, treePruningService);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
