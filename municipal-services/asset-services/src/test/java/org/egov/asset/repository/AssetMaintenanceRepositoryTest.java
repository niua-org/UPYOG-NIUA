// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.config.AssetConfiguration;
import org.egov.asset.kafka.Producer;
import org.egov.asset.repository.querybuilder.AssetMaintenanceQueryBuilder;
import org.egov.asset.repository.rowmapper.AssetMaintenanceRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetMaintenanceRepositoryTest {
    @Mock
    private AssetMaintenanceQueryBuilder queryBuilder;
    @Mock
    private Producer producer;
    @Mock
    private AssetConfiguration config;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private AssetMaintenanceRowMapper rowMapper;

    private AssetMaintenanceRepository subject;

    @BeforeEach
    void setUp() {
        subject = new AssetMaintenanceRepository(queryBuilder, producer, config, jdbcTemplate, rowMapper);
    }


    @Test
    void save_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
