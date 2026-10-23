// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.repository.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ewst.config.EwasteConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EwasteApplicationQueryBuilderTest {
    @Mock
    private EwasteConfiguration configuration;

    private EwasteApplicationQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new EwasteApplicationQueryBuilder(configuration);
    }


    @Test
    void getEwasteApplicationSearchQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
