package org.niua.smoke.support;

import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public final class Polling {

    private Polling() {
    }

    public static void untilTrue(Callable<Boolean> condition, Duration timeout) {
        Awaitility.await()
                .atMost(timeout)
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(condition);
    }

    public static <T> T until(Predicate<T> success, Callable<T> supplier, Duration timeout) {
        return Awaitility.await()
                .atMost(timeout)
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(supplier, success::test);
    }
}
