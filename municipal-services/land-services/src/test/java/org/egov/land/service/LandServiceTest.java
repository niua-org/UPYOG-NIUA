package org.egov.land.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.land.repository.LandRepository;
import org.egov.land.util.LandUtil;
import org.egov.land.validator.LandValidator;
import org.egov.land.web.models.LandInfo;
import org.egov.land.web.models.LandInfoRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandServiceTest {

    @Mock private LandValidator landValidator;
    @Mock private LandEnrichmentService enrichmentService;
    @Mock private LandUserService userService;
    @Mock private LandRepository repository;
    @Mock private LandUtil util;

    @InjectMocks
    private LandService landService;

    @Test
    void create_whenStateLevelTenant_throws() {
        when(util.mDMSCall(any(), any())).thenReturn(new Object());
        LandInfoRequest request = LandInfoRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .landInfo(LandInfo.builder().tenantId("pb").build())
                .build();

        assertThatThrownBy(() -> landService.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("StateLevel");
    }

    @Test
    void update_whenIdMissing_throws() {
        when(util.mDMSCall(any(), any())).thenReturn(new Object());
        LandInfoRequest request = LandInfoRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .landInfo(LandInfo.builder().tenantId("pb.amritsar").build())
                .build();

        assertThatThrownBy(() -> landService.update(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Id is mandatory");
    }
}
