package org.upyog.sv.kafka.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.sv.service.StreetyVendingNotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {
    @Mock
    private StreetyVendingNotificationService notificationService;
    @Mock
    private ObjectMapper mapper;

    private NotificationConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationConsumer(notificationService, mapper);
    }


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
