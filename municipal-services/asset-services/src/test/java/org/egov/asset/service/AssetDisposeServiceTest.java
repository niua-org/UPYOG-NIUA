// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.repository.AssetDisposeRepository;
import org.egov.asset.service.AssetService;
import org.egov.asset.service.EnrichmentService;
import org.egov.asset.util.AssetUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetDisposeServiceTest {
    @Mock
    private AssetDisposeRepository assetDisposeRepository;
    @Mock
    private AssetService assetService;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private AssetUtil assetUtil;

    private AssetDisposeService subject;

    @BeforeEach
    void setUp() {
        subject = new AssetDisposeService(assetDisposeRepository, assetService, enrichmentService, assetUtil);
    }


    @Test
    void createDisposal_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
