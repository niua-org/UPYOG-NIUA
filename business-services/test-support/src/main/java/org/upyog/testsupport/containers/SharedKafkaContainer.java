package org.upyog.testsupport.containers;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Kafka container reused across integration tests in a JVM.
 */
public final class SharedKafkaContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.5.3");

    private static KafkaContainer container;

    private SharedKafkaContainer() {
    }

    public static synchronized KafkaContainer getInstance() {
        if (container == null) {
            container = new KafkaContainer(IMAGE);
            container.start();
        }
        return container;
    }
}
