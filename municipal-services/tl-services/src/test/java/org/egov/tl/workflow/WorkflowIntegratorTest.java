// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.config.TLConfiguration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WorkflowIntegratorTest {
    @Mock
    private RestTemplate rest;
    @Mock
    private TLConfiguration config;

    private WorkflowIntegrator subject;

    @BeforeEach
    void setUp() {
        subject = new WorkflowIntegrator(rest, config);
    }


    @Test
    void callWorkFlow_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
