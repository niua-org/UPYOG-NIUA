// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.service.TradeLicenseService;
import org.egov.tl.service.notification.TLNotificationService;
import org.egov.tl.util.BPANotificationUtil;
import org.egov.tl.util.NotificationUtil;
import org.egov.tl.util.TLRenewalNotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {
    @Mock
    private TLConfiguration config;
    @Mock
    private TradeLicenseService tradeLicenseService;
    @Mock
    private NotificationUtil util;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private BPANotificationUtil bpaNotificationUtil;
    @Mock
    private TLNotificationService tlNotificationService;
    @Mock
    private TLRenewalNotificationUtil tlRenewalNotificationUtil;

    private PaymentNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentNotificationService(config, tradeLicenseService, util, mapper, bpaNotificationUtil, tlNotificationService, tlRenewalNotificationUtil);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
