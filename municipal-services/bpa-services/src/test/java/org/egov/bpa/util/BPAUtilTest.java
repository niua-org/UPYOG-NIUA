// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BPAUtilTest {
    @Mock
    private BPAConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private BPAUtil subject;

    @BeforeEach
    void setUp() {
        subject = new BPAUtil(config, serviceRequestRepository);
    }


    @Test
    void getAuditDetails_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
