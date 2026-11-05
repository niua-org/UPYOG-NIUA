package org.upyog.dashboard.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RetryUtilTest {

    @Test
    @DisplayName("calculateBackoffWithJitter stays within configured max delay")
    void calculateBackoffWithJitter_respectsMaxDelay() {
        long maxDelayMs = 500L;
        for (int attempt = 1; attempt <= 5; attempt++) {
            long backoff = RetryUtil.calculateBackoffWithJitter(attempt, 100L, maxDelayMs);
            assertThat(backoff).isBetween(0L, maxDelayMs);
        }
    }

    @Test
    @DisplayName("calculateBackoffWithJitter handles large attempt without overflow")
    void calculateBackoffWithJitter_largeAttempt_capsAtMax() {
        long backoff = RetryUtil.calculateBackoffWithJitter(100, 1000L, 2000L);
        assertThat(backoff).isBetween(0L, 2000L);
    }
}
