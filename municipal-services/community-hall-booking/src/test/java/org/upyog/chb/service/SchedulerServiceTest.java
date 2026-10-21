// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.repository.CommunityHallBookingRepository;
import org.upyog.chb.service.SchedulerService;
import org.upyog.chb.service.UserService;
import org.upyog.chb.service.WorkflowService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {
    @Mock
    private CommunityHallBookingRepository bookingRepository;
    @Mock
    private UserService userService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private CommunityHallBookingConfiguration config;
    @Mock
    private SchedulerService self;

    private SchedulerService subject;

    @BeforeEach
    void setUp() {
        subject = new SchedulerService(bookingRepository, userService, workflowService, config, self);
    }


    @Test
    void cleanupExpiredEntries_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
