// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.service.BPAService;
import org.egov.bpa.service.notification.BPANotificationService;
import org.egov.bpa.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {
    @Mock
    private BPAConfiguration config;
    @Mock
    private BPAService bpaService;
    @Mock
    private NotificationUtil util;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private BPANotificationService bpaNotificationService;

    private PaymentNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentNotificationService(config, bpaService, util, mapper, bpaNotificationService);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
