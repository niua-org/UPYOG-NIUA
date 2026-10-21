// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.repository.ServiceRequestRepository;
import org.upyog.chb.service.CHBEncryptionService;
import org.upyog.chb.service.UserService;
import org.upyog.chb.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CHBNotificationServiceTest {
    @Mock
    private CommunityHallBookingConfiguration config;
    @Mock
    private NotificationUtil util;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private CHBEncryptionService chbEncryptionService;
    @Mock
    private UserService userService;

    private CHBNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new CHBNotificationService(config, util, serviceRequestRepository, chbEncryptionService, userService);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
