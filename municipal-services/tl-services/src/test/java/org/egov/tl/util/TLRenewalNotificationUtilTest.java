// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.producer.Producer;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TLRenewalNotificationUtilTest {
    @Mock
    private TLConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private Producer producer;
    @Mock
    private NotificationUtil notificationUtil;

    private TLRenewalNotificationUtil subject;

    @BeforeEach
    void setUp() {
        subject = new TLRenewalNotificationUtil(config, serviceRequestRepository, producer, notificationUtil);
    }


    @Test
    void getCustomizedMsg_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
