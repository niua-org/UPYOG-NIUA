package org.egov.tl.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.service.TradeLicenseService;
import org.egov.tl.service.notification.TLNotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TradeLicenseConsumerTest {
    @Mock
    private TLNotificationService notificationService;
    @Mock
    private TradeLicenseService tradeLicenseService;

    private TradeLicenseConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new TradeLicenseConsumer(notificationService, tradeLicenseService);
    }


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
