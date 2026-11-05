// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.notification.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.cdwm.config.CNDConfiguration;
import org.upyog.cdwm.notification.impl.MessageServiceImpl;
import org.upyog.cdwm.notification.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CNDNotificationServiceImplTest {
    @Mock
    private NotificationUtil util;
    @Mock
    private CNDConfiguration config;
    @Mock
    private MessageServiceImpl messageService;

    private CNDNotificationServiceImpl subject;

    @BeforeEach
    void setUp() {
        subject = new CNDNotificationServiceImpl(util, config, messageService);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
