// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.repository.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ndc.config.NDCConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NdcQueryBuilderTest {
    @Mock
    private NDCConfiguration ndcConfig;

    private NdcQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new NdcQueryBuilder(ndcConfig);
    }


    @Test
    void getPaginatedApplicationUuids_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
