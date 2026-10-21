package org.egov.echallan.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.service.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChallanConsumerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private ChallanConfiguration config;

    private ChallanConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new ChallanConsumer(notificationService, config);
    }


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
