// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.service.EDCRService;
import org.egov.bpa.util.BPAUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationUtilTest {
    @Mock
    private BPAConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private Producer producer;
    @Mock
    private EDCRService edcrService;
    @Mock
    private BPAUtil bpaUtil;

    private NotificationUtil subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationUtil(config, serviceRequestRepository, producer, edcrService, bpaUtil);
    }


    @Test
    void getCustomizedMsg_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
