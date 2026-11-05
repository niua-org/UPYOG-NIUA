// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.producer.Producer;
import org.egov.echallan.repository.builder.ChallanQueryBuilder;
import org.egov.echallan.repository.rowmapper.ChallanCountRowMapper;
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
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ChallanCountRowMapper countRowMapper;

    private ChallanRepository subject;

    @BeforeEach
    void setUp() {
        subject = new ChallanRepository(producer, config, queryBuilder, jdbcTemplate, rowMapper, restTemplate, mapper, countRowMapper);
    }


    @Test
    void save_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
