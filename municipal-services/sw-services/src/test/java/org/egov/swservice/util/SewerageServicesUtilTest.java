// generated-by: generate_per_class_unit_tests.py
package org.egov.swservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.swservice.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SewerageServicesUtilTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private SewerageServicesUtil subject;

    @BeforeEach
    void setUp() {
        subject = new SewerageServicesUtil(serviceRequestRepository);
    }


    @Test
    void propertySearch_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
