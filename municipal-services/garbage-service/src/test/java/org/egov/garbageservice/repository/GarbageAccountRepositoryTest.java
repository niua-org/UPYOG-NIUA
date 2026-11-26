// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.garbageservice.kafka.Producer;
import org.egov.garbageservice.repository.builder.GarbageAccountQueryBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GarbageAccountRepositoryTest {
    @Mock
    private Producer producer;
    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private GarbageAccountQueryBuilder queryBuilder;

    private GarbageAccountRepository subject;

    @BeforeEach
    void setUp() {
        subject = new GarbageAccountRepository(producer, namedParameterJdbcTemplate, jdbcTemplate, queryBuilder);
    }


    @Test
    void create_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
