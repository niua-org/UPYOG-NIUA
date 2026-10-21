// generated-by: generate_per_class_unit_tests.py
package org.egov.waterconnection.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.waterconnection.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WaterServicesUtilTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private WaterServicesUtil subject;

    @BeforeEach
    void setUp() {
        subject = new WaterServicesUtil(serviceRequestRepository);
    }


    @Test
    void getAuditDetails_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
