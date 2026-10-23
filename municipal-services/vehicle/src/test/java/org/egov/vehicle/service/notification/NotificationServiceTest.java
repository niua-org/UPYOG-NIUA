// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.vehicle.config.VehicleConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private VehicleConfiguration config;

    private NotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationService(config);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
