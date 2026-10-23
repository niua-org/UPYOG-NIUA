// generated-by: generate_per_class_unit_tests.py
package org.egov.noc.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NOCUtilTest {
    @Mock
    private NOCConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private NOCUtil subject;

    @BeforeEach
    void setUp() {
        subject = new NOCUtil(config, serviceRequestRepository);
    }


    @Test
    void getAuditDetails_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
