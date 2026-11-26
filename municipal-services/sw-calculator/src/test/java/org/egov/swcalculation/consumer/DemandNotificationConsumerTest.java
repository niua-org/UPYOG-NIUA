package org.egov.swcalculation.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.swcalculation.service.SewerageDemandNotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DemandNotificationConsumerTest {
    @Mock
    private SewerageDemandNotificationService notificationService;

    private DemandNotificationConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new DemandNotificationConsumer(notificationService);
    }


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
