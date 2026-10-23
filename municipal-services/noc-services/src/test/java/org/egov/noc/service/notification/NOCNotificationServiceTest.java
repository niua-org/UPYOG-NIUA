// generated-by: generate_per_class_unit_tests.py
package org.egov.noc.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.repository.ServiceRequestRepository;
import org.egov.noc.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NOCNotificationServiceTest {
    @Mock
    private NOCConfiguration config;
    @Mock
    private NotificationUtil util;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private NOCNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new NOCNotificationService(config, util, serviceRequestRepository);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
