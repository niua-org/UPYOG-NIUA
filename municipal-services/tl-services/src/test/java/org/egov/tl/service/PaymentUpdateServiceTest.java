// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.TLRepository;
import org.egov.tl.service.EnrichmentService;
import org.egov.tl.service.TradeLicenseService;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.workflow.WorkflowIntegrator;
import org.egov.tl.workflow.WorkflowService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentUpdateServiceTest {
    @Mock
    private TradeLicenseService tradeLicenseService;
    @Mock
    private TLConfiguration config;
    @Mock
    private TLRepository repository;
    @Mock
    private WorkflowIntegrator wfIntegrator;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private TradeUtil util;

    private PaymentUpdateService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentUpdateService(tradeLicenseService, config, repository, wfIntegrator, enrichmentService, mapper, workflowService, util);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
