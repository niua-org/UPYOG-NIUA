// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.repository.ChallanRepository;
import org.egov.echallan.repository.ServiceRequestRepository;
import org.egov.echallan.service.CalculationService;
import org.egov.echallan.service.EnrichmentService;
import org.egov.echallan.service.UserService;
import org.egov.echallan.util.CommonUtils;
import org.egov.echallan.util.ResponseInfoFactory;
import org.egov.echallan.validator.ChallanValidator;
import org.egov.echallan.workflow.WorkflowIntegrator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChallanServiceTest {
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private UserService userService;
    @Mock
    private ChallanRepository repository;
    @Mock
    private CalculationService calculationService;
    @Mock
    private ChallanValidator validator;
    @Mock
    private CommonUtils utils;
    @Mock
    private ChallanConfiguration config;
    @Mock
    private org.egov.echallan.repository.ServiceRequestRepository serviceRequestRepository;
    @Mock
    private WorkflowIntegrator workflowIntegrator;

    private ChallanService subject;

    @BeforeEach
    void setUp() {
        subject = new ChallanService(enrichmentService, responseInfoFactory, userService, repository, calculationService, validator, utils, config, serviceRequestRepository, workflowIntegrator);
    }


    @Test
    void create_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
