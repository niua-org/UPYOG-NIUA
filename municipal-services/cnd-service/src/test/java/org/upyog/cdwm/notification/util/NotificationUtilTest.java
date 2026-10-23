// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.notification.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.upyog.cdwm.config.CNDConfiguration;
import org.upyog.cdwm.kafka.Producer;
import org.upyog.cdwm.notification.impl.MessageServiceImpl;
import org.upyog.cdwm.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationUtilTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private CNDConfiguration config;
    @Mock
    private MessageServiceImpl messageService;
    @Mock
    private Producer producer;
    @Mock
    private RestTemplate restTemplate;

    private NotificationUtil subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationUtil(serviceRequestRepository, config, messageService, producer, restTemplate);
    }


    @Test
    void getUri_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
