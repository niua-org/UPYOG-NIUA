// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.repository.querybuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.config.AssetConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetMaintenanceQueryBuilderTest {
    @Mock
    private AssetConfiguration config;

    private AssetMaintenanceQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new AssetMaintenanceQueryBuilder(config);
    }


    @Test
    void getMaintenanceSearchQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
