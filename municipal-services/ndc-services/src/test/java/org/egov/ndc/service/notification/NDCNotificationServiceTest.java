// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ndc.config.NDCConfiguration;
import org.egov.ndc.service.UserService;
import org.egov.ndc.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NDCNotificationServiceTest {
    @Mock
    private NDCConfiguration config;
    @Mock
    private NotificationUtil util;
    @Mock
    private UserService userService;

    private NDCNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new NDCNotificationService(config, util, userService);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
