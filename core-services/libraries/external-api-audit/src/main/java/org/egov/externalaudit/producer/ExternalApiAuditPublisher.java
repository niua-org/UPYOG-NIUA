package org.egov.externalaudit.producer;

/**
 * SPI for publishing audit wrappers to Kafka (or a test double).
 * Production implementation is {@link ExternalApiAuditProducer}.
 */
public interface ExternalApiAuditPublisher {

    /**
     * Enqueue an {@link org.egov.externalaudit.model.ExternalApiAuditDetailWrapper} without blocking
     * the business thread. Implementations must be fail-open.
     *
     * @param topic Kafka topic, typically {@code external-api-audit-details}
     * @param event wrapper whose {@code apiAuditDetail} matches persister JSON paths
     */
    void publishAsync(String topic, Object event);
}
