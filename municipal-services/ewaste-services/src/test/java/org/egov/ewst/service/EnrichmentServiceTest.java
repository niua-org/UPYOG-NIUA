// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ewst.config.EwasteConfiguration;
import org.egov.ewst.util.EwasteUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private EwasteConfiguration config;
    @Mock
    private EwasteUtil ewasteUtil;

    private EnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new EnrichmentService(config, ewasteUtil);
    }


    @Test
    void enrichEwasteApplication_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
