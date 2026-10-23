// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.upyog.chb.config.CommunityHallBookingConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WorkflowIntegratorTest {
    @Mock
    private RestTemplate rest;
    @Mock
    private CommunityHallBookingConfiguration config;

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
