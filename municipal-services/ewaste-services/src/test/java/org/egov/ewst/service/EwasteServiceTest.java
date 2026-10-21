// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ewst.config.EwasteConfiguration;
import org.egov.ewst.producer.Producer;
import org.egov.ewst.repository.EwasteApplicationRepository;
import org.egov.ewst.service.EnrichmentService;
import org.egov.ewst.service.WorkflowService;
import org.egov.ewst.validator.EwasteRequestValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EwasteServiceTest {
    @Mock
    private Producer producer;
    @Mock
    private EwasteConfiguration config;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private EwasteRequestValidator validator;
    @Mock
    private WorkflowService wfService;
    @Mock
    private EwasteApplicationRepository ewasteApplicationRepository;

    private EwasteService subject;

    @BeforeEach
    void setUp() {
        subject = new EwasteService(producer, config, enrichmentService, validator, wfService, ewasteApplicationRepository);
    }


    @Test
    void createEwasteRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
