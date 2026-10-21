// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EditNotificationServiceTest {
    @Mock
    private NotificationUtil util;

    private EditNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new EditNotificationService(util);
    }


    @Test
    void sendEditNotification_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
