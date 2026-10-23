// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.util.BPANotificationUtil;
import org.egov.tl.util.NotificationUtil;
import org.egov.tl.util.PropertyUtil;
import org.egov.tl.util.TLRenewalNotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TLNotificationServiceTest {
    @Mock
    private TLConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private NotificationUtil util;
    @Mock
    private BPANotificationUtil bpaNotificationUtil;
    @Mock
    private TLRenewalNotificationUtil tlRenewalNotificationUtil;
    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;
    @Mock
    private PropertyUtil propertyUtil;

    private TLNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new TLNotificationService(config, serviceRequestRepository, util, bpaNotificationUtil, tlRenewalNotificationUtil, centralInstanceUtil, propertyUtil);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
