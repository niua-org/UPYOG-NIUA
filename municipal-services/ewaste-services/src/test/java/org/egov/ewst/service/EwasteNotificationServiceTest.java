// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ewst.config.EwasteConfiguration;
import org.egov.ewst.repository.ServiceRequestRepository;
import org.egov.ewst.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EwasteNotificationServiceTest {
    @Mock
    private EwasteConfiguration config;
    @Mock
    private NotificationUtil util;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private EwasteNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new EwasteNotificationService(config, util, serviceRequestRepository);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
