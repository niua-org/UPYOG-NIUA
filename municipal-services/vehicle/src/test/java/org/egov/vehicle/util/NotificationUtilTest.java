// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.vehicle.config.VehicleConfiguration;
import org.egov.vehicle.producer.VehicleProducer;
import org.egov.vehicle.repository.ServiceRequestRepository;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationUtilTest {
    @Mock
    private VehicleConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private VehicleProducer producer;
    @Mock
    private RestTemplate restTemplate;

    private NotificationUtil subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationUtil(config, serviceRequestRepository, producer, restTemplate);
    }


    @Test
    void getLocalizationMessages_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
