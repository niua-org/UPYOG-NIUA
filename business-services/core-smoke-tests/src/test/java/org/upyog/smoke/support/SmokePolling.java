package org.upyog.smoke.support;

import org.awaitility.Awaitility;

import java.util.function.Supplier;

public final class SmokePolling {

    private SmokePolling() {
    }

    public static void untilTrue(Supplier<Boolean> condition, String description) {
        Awaitility.await(description)
                .atMost(SmokeConfig.HEALTH_POLL_TIMEOUT)
                .pollInterval(SmokeConfig.HEALTH_POLL_INTERVAL)
                .until(condition::get);
    }
}
