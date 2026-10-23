// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ndc.config.NDCConfiguration;
import org.egov.ndc.producer.Producer;
import org.egov.ndc.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationUtilTest {
    @Mock
    private NDCConfiguration config;
    @Mock
    private Producer producer;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private NotificationUtil subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationUtil(config, producer, serviceRequestRepository);
    }


    @Test
    void sendSMS_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
