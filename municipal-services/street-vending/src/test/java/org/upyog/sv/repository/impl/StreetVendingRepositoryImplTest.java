// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.upyog.sv.config.StreetVendingConfiguration;
import org.upyog.sv.kafka.producer.Producer;
import org.upyog.sv.repository.querybuilder.StreetVendingQueryBuilder;
import org.upyog.sv.repository.rowmapper.StreetVendingApplicationRowMapper;
import org.upyog.sv.repository.rowmapper.StreetVendingDraftApplicationRowMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StreetVendingRepositoryImplTest {
    @Mock
    private Producer producer;
    @Mock
    private StreetVendingConfiguration vendingConfiguration;
    @Mock
    private StreetVendingQueryBuilder queryBuilder;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private StreetVendingApplicationRowMapper rowMapper;
    @Mock
    private StreetVendingDraftApplicationRowMapper draftApplicationRowMapper;
    @Mock
    private ObjectMapper objectMapper;

    private StreetVendingRepositoryImpl subject;

    @BeforeEach
    void setUp() {
        subject = new StreetVendingRepositoryImpl(producer, vendingConfiguration, queryBuilder, jdbcTemplate, rowMapper, draftApplicationRowMapper, objectMapper);
    }


    @Test
    void save_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
