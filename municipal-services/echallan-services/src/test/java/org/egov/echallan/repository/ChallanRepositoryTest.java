// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.producer.Producer;
import org.egov.echallan.repository.builder.ChallanQueryBuilder;
import org.egov.echallan.repository.rowmapper.ChallanRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChallanRepositoryTest {
    @Mock
    private Producer producer;
    @Mock
    private ChallanConfiguration config;
    @Mock
    private ChallanQueryBuilder queryBuilder;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ChallanRowMapper rowMapper;
    @Mock
    private RestTemplate restTemplate;

    private ChallanRepository subject;

    @BeforeEach
    void setUp() {
        subject = new ChallanRepository(producer, config, queryBuilder, jdbcTemplate, rowMapper, restTemplate);
    }


    @Test
    void save_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
