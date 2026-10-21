// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.upyog.chb.config.CommunityHallBookingConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentTimerRedisServiceTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private CommunityHallBookingConfiguration bookingConfiguration;

    private PaymentTimerRedisService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentTimerRedisService(redis, bookingConfiguration);
    }


    @Test
    void tryAcquireSlot_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
