// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.repository.AssetRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetUtilTest {
    @Mock
    private AssetRepository assetRepository;

    private AssetUtil subject;

    @BeforeEach
    void setUp() {
        subject = new AssetUtil(assetRepository);
    }


    @Test
    void getAuditDetails_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
