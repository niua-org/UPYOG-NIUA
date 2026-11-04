// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.calculator.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CustomDepreciationRepositoryImplTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    private CustomDepreciationRepositoryImpl subject;

    @BeforeEach
    void setUp() {
        subject = new CustomDepreciationRepositoryImpl(jdbcTemplate);
    }


    @Test
    void executeBulkDepreciationProcedure_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
