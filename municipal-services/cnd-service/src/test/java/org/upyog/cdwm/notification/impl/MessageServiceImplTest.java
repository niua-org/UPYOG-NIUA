// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.notification.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.cdwm.notification.util.NotificationUtil;
import org.upyog.cdwm.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private NotificationUtil util;

    private MessageServiceImpl subject;

    @BeforeEach
    void setUp() {
        subject = new MessageServiceImpl(serviceRequestRepository, util);
    }


    @Test
    void getCustomizedMsg_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
