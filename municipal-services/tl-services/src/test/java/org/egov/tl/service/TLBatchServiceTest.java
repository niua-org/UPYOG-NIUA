// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.producer.Producer;
import org.egov.tl.repository.TLRepository;
import org.egov.tl.service.EnrichmentService;
import org.egov.tl.util.NotificationUtil;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.workflow.WorkflowIntegrator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TLBatchServiceTest {
    @Mock
    private NotificationUtil util;
    @Mock
    private TLConfiguration config;
    @Mock
    private TLRepository repository;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private WorkflowIntegrator workflowIntegrator;
    @Mock
    private Producer producer;
    @Mock
    private TradeUtil tradeUtil;

    private TLBatchService subject;

    @BeforeEach
    void setUp() {
        subject = new TLBatchService(util, config, repository, enrichmentService, workflowIntegrator, producer, tradeUtil);
    }


    @Test
    void getLicensesAndPerformAction_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
