// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.fsm.config.FSMConfiguration;
import org.egov.fsm.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private FSMConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private NotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationService(config, serviceRequestRepository);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
