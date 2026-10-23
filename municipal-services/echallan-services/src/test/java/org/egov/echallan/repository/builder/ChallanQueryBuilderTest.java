// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.repository.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallan.config.ChallanConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChallanQueryBuilderTest {
    @Mock
    private ChallanConfiguration config;

    private ChallanQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new ChallanQueryBuilder(config);
    }


    @Test
    void getChallanSearchQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
