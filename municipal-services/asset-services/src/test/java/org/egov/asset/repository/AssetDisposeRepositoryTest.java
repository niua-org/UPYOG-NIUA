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
import org.egov.asset.repository.querybuilder.AssetDisposalQueryBuilder;
import org.egov.asset.repository.rowmapper.AssetDisposalRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetDisposeRepositoryTest {
    @Mock
    private Producer producer;
    @Mock
    private AssetConfiguration config;
    @Mock
    private AssetDisposalQueryBuilder queryBuilder;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private AssetDisposalRowMapper assetDisposalRowMapper;

    private AssetDisposeRepository subject;

    @BeforeEach
    void setUp() {
        subject = new AssetDisposeRepository(producer, config, queryBuilder, jdbcTemplate, assetDisposalRowMapper);
    }


    @Test
    void save_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
