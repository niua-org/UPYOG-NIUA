// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.pgr.repository.rowmapper.PGRQueryBuilder;
import org.egov.pgr.repository.rowmapper.PGRRowMapper;
import org.egov.pgr.util.PGRUtils;
import org.springframework.jdbc.core.JdbcTemplate;

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
