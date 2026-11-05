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
import org.egov.asset.repository.querybuilder.AssetQueryBuilder;
import org.egov.asset.repository.rowmapper.AssetLimitedDateRowMapper;
import org.egov.asset.repository.rowmapper.AssetRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetRepositoryTest {
    @Mock
    private AssetQueryBuilder queryBuilder;
    @Mock
    private AssetConfiguration config;
    @Mock
    private Producer producer;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private AssetRowMapper rowMapper;
    @Mock
    private AssetLimitedDateRowMapper assetLimitedDateRowMapper;

    private AssetRepository subject;

    @BeforeEach
    void setUp() {
        subject = new AssetRepository(queryBuilder, config, producer, jdbcTemplate, rowMapper, assetLimitedDateRowMapper);
    }


    @Test
    void save_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
