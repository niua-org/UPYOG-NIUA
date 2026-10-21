// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private ChallanConfiguration config;
    @Mock
    private NotificationUtil util;

    private NotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationService(config, util);
    }


    @Test
    void sendChallanNotification_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
