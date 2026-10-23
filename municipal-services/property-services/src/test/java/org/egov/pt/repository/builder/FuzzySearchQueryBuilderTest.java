// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.repository.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pt.config.PropertyConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FuzzySearchQueryBuilderTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private PropertyConfiguration config;

    private FuzzySearchQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new FuzzySearchQueryBuilder(mapper, config);
    }


    @Test
    void getFuzzySearchQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
