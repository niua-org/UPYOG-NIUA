// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.producer.PropertyProducer;
import org.egov.pt.repository.ServiceRequestRepository;
import org.egov.pt.service.UserService;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationUtilTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private PropertyConfiguration config;
    @Mock
    private PropertyProducer producer;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private UserService userService;

    private NotificationUtil subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationUtil(serviceRequestRepository, config, producer, restTemplate, userService);
    }


    @Test
    void getMessageTemplate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
