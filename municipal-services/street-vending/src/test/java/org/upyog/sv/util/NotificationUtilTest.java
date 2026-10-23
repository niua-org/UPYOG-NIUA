// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.upyog.sv.config.StreetVendingConfiguration;
import org.upyog.sv.kafka.producer.Producer;
import org.upyog.sv.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationUtilTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private StreetVendingConfiguration config;
    @Mock
    private Producer producer;
    @Mock
    private RestTemplate restTemplate;

    private NotificationUtil subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationUtil(serviceRequestRepository, config, producer, restTemplate);
    }


    @Test
    void getMessageTemplate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
