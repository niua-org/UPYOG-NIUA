// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.upyog.cdwm.config.CNDConfiguration;
import org.upyog.cdwm.kafka.Producer;
import org.upyog.cdwm.repository.querybuilder.CNDServiceQueryBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CNDServiceRepositoryImplTest {
    @Mock
    private CNDConfiguration config;
    @Mock
    private Producer producer;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private CNDServiceQueryBuilder queryBuilder;

    private CNDServiceRepositoryImpl subject;

    @BeforeEach
    void setUp() {
        subject = new CNDServiceRepositoryImpl(config, producer, jdbcTemplate, queryBuilder);
    }


    @Test
    void saveCNDApplicationDetail_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
