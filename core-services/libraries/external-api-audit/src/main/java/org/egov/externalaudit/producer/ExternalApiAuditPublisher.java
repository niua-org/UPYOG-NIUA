package org.egov.externalaudit.producer;

public interface ExternalApiAuditPublisher {

    void publishAsync(String topic, Object event);
}
