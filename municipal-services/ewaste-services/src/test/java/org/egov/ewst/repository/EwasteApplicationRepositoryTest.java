// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ewst.repository.builder.EwasteApplicationQueryBuilder;
import org.egov.ewst.repository.rowmapper.EwasteApplicationRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EwasteApplicationRepositoryTest {
    @Mock
    private EwasteApplicationQueryBuilder queryBuilder1;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private EwasteApplicationRowMapper eRowMapper;

    private EwasteApplicationRepository subject;

    @BeforeEach
    void setUp() {
        subject = new EwasteApplicationRepository(queryBuilder1, jdbcTemplate, eRowMapper);
    }


    @Test
    void getApplication_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
