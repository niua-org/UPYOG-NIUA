// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.repository.querybuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.cdwm.config.CNDConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CNDServiceQueryBuilderTest {
    @Mock
    private CNDConfiguration cndServiceConfiguration;

    private CNDServiceQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new CNDServiceQueryBuilder(cndServiceConfiguration);
    }


    @Test
    void getCNDApplicationQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
