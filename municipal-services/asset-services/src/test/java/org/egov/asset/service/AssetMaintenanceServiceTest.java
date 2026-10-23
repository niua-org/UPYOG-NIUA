// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.repository.AssetMaintenanceRepository;
import org.egov.asset.service.AssetService;
import org.egov.asset.service.EnrichmentService;
import org.egov.asset.util.AssetUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetMaintenanceServiceTest {
    @Mock
    private AssetMaintenanceRepository assetMaintenanceRepository;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private AssetUtil assetUtil;
    @Mock
    private AssetService assetService;

    private AssetMaintenanceService subject;

    @BeforeEach
    void setUp() {
        subject = new AssetMaintenanceService(assetMaintenanceRepository, enrichmentService, assetUtil, assetService);
    }


    @Test
    void createMaintenance_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
