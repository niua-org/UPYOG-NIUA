package com.example.gateway.config;

import com.example.gateway.metrics.GatewayCpuFixMetrics;
import com.example.gateway.utils.UserAuthCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GatewayCpuFixDiagnostics {

    private static final String LOG_PREFIX = "GATEWAY_CPU_FIX";

    private final UserAuthCache userAuthCache;
    private final GatewayCpuFixMetrics metrics;

    private final List<String> customAsyncUris;
    private final long userAuthCacheTtlSeconds;
    private final long userAuthCacheMaxSize;
    private final long kafkaProducerMaxBlockMs;

    public GatewayCpuFixDiagnostics(
            UserAuthCache userAuthCache,
            GatewayCpuFixMetrics metrics,
            @Value("#{'${egov.custom.async.uris}'.split(',')}") List<String> customAsyncUris,
            @Value("${egov.gateway.user-auth-cache-ttl-seconds}") long userAuthCacheTtlSeconds,
            @Value("${egov.gateway.user-auth-cache-max-size}") long userAuthCacheMaxSize,
            @Value("${spring.kafka.producer.properties.max.block.ms}") long kafkaProducerMaxBlockMs) {
        this.userAuthCache = userAuthCache;
        this.metrics = metrics;
        this.customAsyncUris = customAsyncUris.stream().map(String::trim).collect(Collectors.toList());
        this.userAuthCacheTtlSeconds = userAuthCacheTtlSeconds;
        this.userAuthCacheMaxSize = userAuthCacheMaxSize;
        this.kafkaProducerMaxBlockMs = kafkaProducerMaxBlockMs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupBanner() {
        log.info(
                "{}: Gateway CPU-fix build active; customAsyncUris={}; userAuthCacheTtlSeconds={}; userAuthCacheMaxSize={}; kafkaProducerMaxBlockMs={}",
                LOG_PREFIX,
                customAsyncUris,
                userAuthCacheTtlSeconds,
                userAuthCacheMaxSize,
                kafkaProducerMaxBlockMs);
    }

    @Scheduled(fixedRateString = "${egov.gateway.cpu-fix-metrics-interval-ms}")
    public void logPeriodicSummary() {
        UserAuthCache.StatsSnapshot cacheStats = userAuthCache.snapshotStats();
        long skipped = metrics.drainCustomAsyncSkippedSinceLastReport();
        long publishAttempts = metrics.drainCustomAsyncPublishAttemptsSinceLastReport();
        long publishSuccess = metrics.drainCustomAsyncPublishSuccessSinceLastReport();
        long publishFailure = metrics.drainCustomAsyncPublishFailureSinceLastReport();

        log.info(
                "{}: periodic summary (last interval) userAuthCacheSize={} userAuthCacheHitRate={} userAuthCacheEvictions={} userAuthCacheInvalidations={} customAsyncSkipped={} customAsyncPublishAttempts={} customAsyncPublishSuccess={} customAsyncPublishFailure={}",
                LOG_PREFIX,
                cacheStats.estimatedSize(),
                String.format("%.4f", cacheStats.hitRate()),
                cacheStats.evictionCount(),
                cacheStats.invalidations(),
                skipped,
                publishAttempts,
                publishSuccess,
                publishFailure);
    }
}
