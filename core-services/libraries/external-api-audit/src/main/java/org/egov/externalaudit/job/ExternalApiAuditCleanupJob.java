package org.egov.externalaudit.job;

import lombok.extern.slf4j.Slf4j;
import org.egov.externalaudit.config.ExternalApiAuditProperties;
import org.egov.externalaudit.repository.IntegrationAuditRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes audit rows older than the configured retention window.
 * Enable only on the service whose datasource hosts {@code ug_external_api_*} tables.
 */
@Slf4j
@Component
@ConditionalOnBean(IntegrationAuditRepository.class)
@ConditionalOnProperty(prefix = "external.api.audit.cleanup", name = "enabled", havingValue = "true")
public class ExternalApiAuditCleanupJob {

    private final IntegrationAuditRepository integrationAuditRepository;
    private final ExternalApiAuditProperties properties;

    public ExternalApiAuditCleanupJob(IntegrationAuditRepository integrationAuditRepository,
            ExternalApiAuditProperties properties) {
        this.integrationAuditRepository = integrationAuditRepository;
        this.properties = properties;
    }

    @Scheduled(cron = "${external.api.audit.cleanup.cron:0 30 3 * * *}",
            zone = "${external.api.audit.cleanup.zone:Asia/Kolkata}")
    public void deleteExpiredRecords() {
        long createdTimeThreshold = System.currentTimeMillis() - properties.getCleanup().getRetentionMs();
        int deleted = integrationAuditRepository.deleteExpiredAuditRecords(createdTimeThreshold);
        log.info("Integration audit cleanup job completed. Deleted {} rows.", deleted);
    }
}
