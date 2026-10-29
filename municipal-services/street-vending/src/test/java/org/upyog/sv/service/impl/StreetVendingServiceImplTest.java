package org.upyog.sv.service.impl;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.upyog.sv.repository.StreetVendingRepository;
import org.upyog.sv.service.DemandService;
import org.upyog.sv.service.EnrichmentService;
import org.upyog.sv.service.MdmsCacheService;
import org.upyog.sv.service.StreetVendingEncryptionService;
import org.upyog.sv.service.WorkflowService;
import org.upyog.sv.util.MdmsUtil;
import org.upyog.sv.validator.StreetVendingValidator;
import org.upyog.sv.web.models.StreetVendingDetail;
import org.upyog.sv.web.models.StreetVendingRequest;
import org.upyog.sv.web.models.StreetVendingSearchCriteria;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreetVendingServiceImplTest {

    @Mock private MdmsUtil util;
    @Mock private EnrichmentService enrichmentService;
    @Mock private StreetVendingRepository streetVendingRepository;
    @Mock private DemandService demandService;
    @Mock private WorkflowService workflowService;
    @Mock private StreetVendingValidator validator;
    @Mock private StreetVendingEncryptionService encryptionService;
    @Mock private MdmsCacheService mdmsCache;

    @InjectMocks
    private StreetVendingServiceImpl streetVendingService;

    @Test
    void createStreetVendingApplication_whenStateLevelTenant_throws() {
        StreetVendingRequest request = StreetVendingRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .streetVendingDetail(StreetVendingDetail.builder().tenantId("pb").build())
                .build();
        when(util.mDMSCall(any(), any())).thenReturn(Collections.emptyMap());

        assertThatThrownBy(() -> streetVendingService.createStreetVendingApplication(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("StateLevel");
    }

    @Test
    void getStreetVendingDetails_delegatesToRepository() {
        StreetVendingSearchCriteria criteria = StreetVendingSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(streetVendingRepository.getStreetVendingApplications(criteria)).thenReturn(Collections.emptyList());

        assertThat(streetVendingService.getStreetVendingDetails(RequestInfo.builder().build(), criteria)).isEmpty();
        verify(streetVendingRepository).getStreetVendingApplications(criteria);
    }
}
