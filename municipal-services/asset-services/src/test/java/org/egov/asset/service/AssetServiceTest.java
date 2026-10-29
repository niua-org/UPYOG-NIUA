package org.egov.asset.service;

import org.egov.asset.repository.AssetRepository;
import org.egov.asset.util.AssetValidator;
import org.egov.asset.util.MdmsUtil;
import org.egov.asset.web.models.Asset;
import org.egov.asset.web.models.AssetRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock private MdmsUtil util;
    @Mock private AssetRepository assetRepository;
    @Mock private AssetValidator assetValidator;
    @Mock private EnrichmentService enrichmentService;
    @Mock private WorkflowService workflowService;
    @Mock private ModelMapper modelMapper;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private AssetService assetService;

    @Test
    void create_whenStateLevelTenant_throws() {
        when(util.mDMSCall(any(), any())).thenReturn(new Object());
        AssetRequest request = AssetRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .asset(Asset.builder().tenantId("pb").build())
                .build();

        assertThatThrownBy(() -> assetService.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("StateLevel");
    }
}
