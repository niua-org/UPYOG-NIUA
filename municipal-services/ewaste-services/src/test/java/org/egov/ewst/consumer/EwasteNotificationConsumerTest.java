package org.egov.ewst.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.ewst.service.EwasteNotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EwasteNotificationConsumerTest {
    @Mock
    private EwasteNotificationService notificationService1;
    @Mock
    private ObjectMapper mapper;

    private EwasteNotificationConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new EwasteNotificationConsumer(notificationService1, mapper);
    }


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
