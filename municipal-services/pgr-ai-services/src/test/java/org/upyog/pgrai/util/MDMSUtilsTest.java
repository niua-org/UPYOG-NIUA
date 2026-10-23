// generated-by: generate_per_class_unit_tests.py
package org.upyog.pgrai.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.pgrai.config.PGRConfiguration;
import org.upyog.pgrai.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MDMSUtilsTest {
    @Mock
    private PGRConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private MDMSUtils subject;

    @BeforeEach
    void setUp() {
        subject = new MDMSUtils(config, serviceRequestRepository);
    }


    @Test
    void mDMSCall_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
