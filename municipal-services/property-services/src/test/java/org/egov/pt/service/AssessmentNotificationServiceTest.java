// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.service.BillingService;
import org.egov.pt.service.PropertyService;
import org.egov.pt.util.NotificationUtil;
import org.egov.pt.util.UnmaskingUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssessmentNotificationServiceTest {
    @Mock
    private NotificationUtil util;
    @Mock
    private PropertyService propertyService;
    @Mock
    private PropertyConfiguration config;
    @Mock
    private BillingService billingService;
    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;
    @Mock
    private UnmaskingUtil unmaskingUtil;

    private AssessmentNotificationService subject;

    @BeforeEach
    void setUp() {
        subject = new AssessmentNotificationService(util, propertyService, config, billingService, centralInstanceUtil, unmaskingUtil);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
