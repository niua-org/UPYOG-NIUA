package org.egov.ndc.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ndc.service.notification.NDCNotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NDCConsumerTest {
    @Mock
    private NDCNotificationService notificationService;

    private NDCConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new NDCConsumer(notificationService);
    }


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
