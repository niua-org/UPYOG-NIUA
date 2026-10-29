package org.upyog.cdwm.service.impl;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.cdwm.config.CNDConfiguration;
import org.upyog.cdwm.repository.impl.CNDServiceRepositoryImpl;
import org.upyog.cdwm.service.CalculationService;
import org.upyog.cdwm.service.EnrichmentService;
import org.upyog.cdwm.service.UserService;
import org.upyog.cdwm.service.WorkflowService;
import org.upyog.cdwm.web.models.CNDServiceSearchCriteria;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CNDServiceImplTest {

    @Mock private EnrichmentService enrichmentService;
    @Mock private WorkflowService workflowService;
    @Mock private CalculationService calculationService;
    @Mock private CNDServiceRepositoryImpl cndApplicationRepository;
    @Mock private UserService userService;
    @Mock private CNDConfiguration config;

    @InjectMocks
    private CNDServiceImpl cndService;

    @Test
    void getCNDApplicationDetails_whenRepositoryEmpty_returnsEmpty() {
        CNDServiceSearchCriteria criteria = CNDServiceSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(cndApplicationRepository.getCNDApplicationDetail(criteria)).thenReturn(Collections.emptyList());

        assertThat(cndService.getCNDApplicationDetails(RequestInfo.builder().build(), criteria)).isEmpty();
    }
}
