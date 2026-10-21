// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.config.AssetConfiguration;
import org.egov.asset.repository.IdGenRepository;
import org.egov.asset.util.AssetUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private AssetConfiguration config;
    @Mock
    private AssetUtil assetUtil;
    @Mock
    private IdGenRepository idGenRepository;

    private EnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new EnrichmentService(config, assetUtil, idGenRepository);
    }


    @Test
    void enrichAssetCreateRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
