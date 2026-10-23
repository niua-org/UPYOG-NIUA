// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.producer.Producer;
import org.egov.echallan.repository.ServiceRequestRepository;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationUtilTest {
    @Mock
    private ChallanConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private Producer producer;

    private NotificationUtil subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationUtil(config, serviceRequestRepository, restTemplate, producer);
    }


    @Test
    void getShortenedUrl_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
