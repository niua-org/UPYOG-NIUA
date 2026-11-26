package upyog.service.impl;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import upyog.repository.EstateRepository;
import upyog.service.AssetService;
import upyog.service.DemandService;
import upyog.service.EnrichmentService;
import upyog.service.UserService;
import upyog.config.EstateConfiguration;
import upyog.web.models.AssetRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EstateServiceImplTest {

    @Mock private EnrichmentService enrichmentService;
    @Mock private EstateRepository estateRepository;
    @Mock private UserService userService;
    @Mock private EstateConfiguration estateConfiguration;
    @Mock private AssetService assetService;
    @Mock private DemandService demandService;

    @InjectMocks
    private EstateServiceImpl estateService;

    @Test
    void createAsset_whenNoAssets_throws() {
        AssetRequest request = new AssetRequest();
        request.setRequestInfo(RequestInfo.builder().build());

        assertThatThrownBy(() -> estateService.createAsset(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("at least one asset");
    }
}
