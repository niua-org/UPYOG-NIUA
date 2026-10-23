// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.repository.rowmapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.pgr.config.PGRConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PGRQueryBuilderTest {
    @Mock
    private PGRConfiguration config;

    private PGRQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new PGRQueryBuilder(config);
    }


    @Test
    void getPGRSearchQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
