package org.egov.core.smoke.support;

import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Health polling budget is 90 seconds, inside the 60–90s smoke window.
 */
public final class Polling {

    public static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(90);
    public static final Duration POLL_INTERVAL = Duration.ofSeconds(3);

    private Polling() {
    }

    public static void until(Callable<Boolean> probe) {
        Awaitility.await()
                .atMost(HEALTH_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .ignoreExceptions()
                .until(probe);
    }
}
