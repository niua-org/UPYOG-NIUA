// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.repository.ServiceRequestRepository;
import org.upyog.adv.service.ADVEncryptionService;
import org.upyog.adv.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ADVNotificationServiceTest {
    @Mock
    private BookingConfiguration config;
    @Mock
    private NotificationUtil util;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ADVEncryptionService advEncryptionService;

    private ADVNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new ADVNotificationService(config, util, serviceRequestRepository, advEncryptionService);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
