package org.upyog.rs.service.impl;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.rs.config.RequestServiceConfiguration;
import org.upyog.rs.repository.RequestServiceRepository;
import org.upyog.rs.service.DemandService;
import org.upyog.rs.service.EnrichmentService;
import org.upyog.rs.service.UserService;
import org.upyog.rs.service.WorkflowService;
import org.upyog.rs.web.models.waterTanker.WaterTankerBookingSearchCriteria;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaterTankerServiceImplTest {

    @Mock private EnrichmentService enrichmentService;
    @Mock private RequestServiceRepository requestServiceRepository;
    @Mock private WorkflowService workflowService;
    @Mock private DemandService demandService;
    @Mock private RequestServiceConfiguration config;
    @Mock private UserService userService;

    @InjectMocks
    private WaterTankerServiceImpl waterTankerService;

    @Test
    void getWaterTankerBookingDetails_whenRepositoryEmpty_returnsEmptyList() {
        WaterTankerBookingSearchCriteria criteria = WaterTankerBookingSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(requestServiceRepository.getWaterTankerBookingDetails(criteria)).thenReturn(Collections.emptyList());

        assertThat(waterTankerService.getWaterTankerBookingDetails(RequestInfo.builder().build(), criteria)).isEmpty();
    }
}
