// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BPANotificationServiceTest {
    @Mock
    private BPAConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private NotificationUtil util;
    @Mock
    private BPAUtil bpaUtil;

    private BPANotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new BPANotificationService(config, serviceRequestRepository, util, bpaUtil);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
