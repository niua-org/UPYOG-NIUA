// generated-by: generate_per_class_unit_tests.py
package org.upyog.pgrai.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.upyog.pgrai.repository.rowmapper.PGRQueryBuilder;
import org.upyog.pgrai.repository.rowmapper.PGRRowMapper;
import org.upyog.pgrai.util.PGRUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PGRRepositoryTest {
    @Mock
    private PGRQueryBuilder queryBuilder;
    @Mock
    private PGRRowMapper rowMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PGRUtils utils;

    private PGRRepository subject;

    @BeforeEach
    void setUp() {
        subject = new PGRRepository(queryBuilder, rowMapper, jdbcTemplate, utils);
    }


    @Test
    void getServiceWrappers_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
