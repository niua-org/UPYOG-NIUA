// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.sv.repository.StreetVendingRepository;
import org.upyog.sv.service.PaymentService;
import org.upyog.sv.service.StreetyVendingNotificationService;
import org.upyog.sv.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {
    @Mock
    private StreetVendingRepository streetVendingRepository;
    @Mock
    private UserService userService;
    @Mock
    private PaymentService service;
    @Mock
    private StreetyVendingNotificationService notificationService;

    private SchedulerService subject;

    @BeforeEach
    void setUp() {
        subject = new SchedulerService(streetVendingRepository, userService, service, notificationService);
    }


    @Test
    void processStreetVendingApplications_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
