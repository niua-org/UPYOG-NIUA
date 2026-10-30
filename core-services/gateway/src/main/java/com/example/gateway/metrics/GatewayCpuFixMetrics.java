package com.example.gateway.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class GatewayCpuFixMetrics {

    private final AtomicLong customAsyncSkipped = new AtomicLong();
    private final AtomicLong customAsyncPublishAttempts = new AtomicLong();
    private final AtomicLong customAsyncPublishSuccess = new AtomicLong();
    private final AtomicLong customAsyncPublishFailure = new AtomicLong();

    public void recordCustomAsyncSkipped() {
        customAsyncSkipped.incrementAndGet();
    }

    public void recordCustomAsyncPublishAttempt() {
        customAsyncPublishAttempts.incrementAndGet();
    }

    public void recordCustomAsyncPublishSuccess() {
        customAsyncPublishSuccess.incrementAndGet();
    }

    public void recordCustomAsyncPublishFailure() {
        customAsyncPublishFailure.incrementAndGet();
    }

    public long drainCustomAsyncSkippedSinceLastReport() {
        return customAsyncSkipped.getAndSet(0);
    }

    public long drainCustomAsyncPublishAttemptsSinceLastReport() {
        return customAsyncPublishAttempts.getAndSet(0);
    }

    public long drainCustomAsyncPublishSuccessSinceLastReport() {
        return customAsyncPublishSuccess.getAndSet(0);
    }

    public long drainCustomAsyncPublishFailureSinceLastReport() {
        return customAsyncPublishFailure.getAndSet(0);
    }
}
