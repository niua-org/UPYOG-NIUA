package org.egov.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.config.ApportionConfig;
import org.egov.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MDMSServiceTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ApportionConfig config;

    @InjectMocks
    private MDMSService mdmsService;

    @Test
    void getMdmsSearchUrl_concatenatesHostAndEndpoint() {
        when(config.getMdmsHost()).thenReturn("http://mdms/");
        when(config.getMdmsEndPoint()).thenReturn("v1/_search");

        assertThat(mdmsService.getMdmsSearchUrl().toString()).isEqualTo("http://mdms/v1/_search");
    }

    @Test
    void mDMSCall_delegatesToRepository() {
        when(config.getMdmsHost()).thenReturn("http://mdms/");
        when(config.getMdmsEndPoint()).thenReturn("v1/_search");
        Object payload = new Object();
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(payload);

        assertThat(mdmsService.mDMSCall(new RequestInfo(), "pb")).isSameAs(payload);
    }
}
