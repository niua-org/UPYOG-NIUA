// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.TLRepository;
import org.egov.tl.service.CalculationService;
import org.egov.tl.service.DiffService;
import org.egov.tl.service.EnrichmentService;
import org.egov.tl.service.TLBatchService;
import org.egov.tl.service.UserService;
import org.egov.tl.service.notification.EditNotificationService;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.validator.TLValidator;
import org.egov.tl.workflow.ActionValidator;
import org.egov.tl.workflow.TLWorkflowService;
import org.egov.tl.workflow.WorkflowIntegrator;
import org.egov.tl.workflow.WorkflowService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TradeLicenseServiceTest {
    @Mock
    private WorkflowIntegrator wfIntegrator;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private UserService userService;
    @Mock
    private TLRepository repository;
    @Mock
    private ActionValidator actionValidator;
    @Mock
    private TLValidator tlValidator;
    @Mock
    private TLWorkflowService TLWorkflowService;
    @Mock
    private CalculationService calculationService;
    @Mock
    private TradeUtil util;
    @Mock
    private DiffService diffService;
    @Mock
    private TLConfiguration config;
    @Mock
    private EditNotificationService editNotificationService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private TradeUtil tradeUtil;
    @Mock
    private TLBatchService tlBatchService;

    private TradeLicenseService subject;

    @BeforeEach
    void setUp() {
        subject = new TradeLicenseService(wfIntegrator, enrichmentService, userService, repository, actionValidator, tlValidator, TLWorkflowService, calculationService, util, diffService, config, editNotificationService, workflowService, tradeUtil, tlBatchService);
    }


    @Test
    void create_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
