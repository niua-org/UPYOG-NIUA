package org.egov.bpa.service;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.IdGenRepository;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.validator.MDMSValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {

    @Mock private BPAConfiguration config;
    @Mock private BPAUtil bpaUtil;
    @Mock private IdGenRepository idGenRepository;
    @Mock private WorkflowService workflowService;
    @Mock private EDCRService edcrService;
    @Mock private WorkflowIntegrator wfIntegrator;
    @Mock private NocService nocService;
    @Mock private BPAUtil util;
    @Mock private UserService userService;
    @Mock private MDMSValidator mdmsValidator;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private MultiStateInstanceUtil centralInstanceUtil;

    @InjectMocks
    private EnrichmentService enrichmentService;

    @Test
    void skipPayment_whenDemandZero_triggersSkipPayWorkflow() {
        BPA bpa = BPA.builder().applicationNo("BPA-1").build();
        bpa.setWorkflow(Workflow.builder().action(BPAConstants.ACTION_APPROVE).build());
        BPARequest request = BPARequest.builder().BPA(bpa).build();
        when(bpaUtil.getDemandAmount(request)).thenReturn(BigDecimal.ZERO);

        enrichmentService.skipPayment(request);

        verify(wfIntegrator).callWorkFlow(request);
    }

    @Test
    void skipPayment_whenDemandPositive_doesNotCallWorkflow() {
        BPA bpa = BPA.builder().applicationNo("BPA-1").build();
        bpa.setWorkflow(Workflow.builder().action(BPAConstants.ACTION_APPROVE).build());
        BPARequest request = BPARequest.builder().BPA(bpa).build();
        when(bpaUtil.getDemandAmount(request)).thenReturn(BigDecimal.TEN);

        enrichmentService.skipPayment(request);

        verify(wfIntegrator, never()).callWorkFlow(any());
    }
}
