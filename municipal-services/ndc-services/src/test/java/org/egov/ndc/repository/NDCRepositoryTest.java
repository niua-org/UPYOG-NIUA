// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ndc.repository.builder.NdcQueryBuilder;
import org.egov.ndc.repository.rowmapper.NdcRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NDCRepositoryTest {
    @Mock
    private NdcQueryBuilder queryBuilder;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private NdcRowMapper rowMapper;

    private NDCRepository subject;

    @BeforeEach
    void setUp() {
        subject = new NDCRepository(queryBuilder, jdbcTemplate, rowMapper);
    }


    @Test
    void getExistingUuids_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
