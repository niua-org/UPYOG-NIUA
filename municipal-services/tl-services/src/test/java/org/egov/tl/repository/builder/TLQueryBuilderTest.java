// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.repository.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.config.TLConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TLQueryBuilderTest {
    @Mock
    private TLConfiguration config;

    private TLQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new TLQueryBuilder(config);
    }


    @Test
    void getTLSearchQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
