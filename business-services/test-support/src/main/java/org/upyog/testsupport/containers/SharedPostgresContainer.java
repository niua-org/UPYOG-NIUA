package org.upyog.testsupport.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Postgres container reused across integration tests in a JVM to reduce startup time.
 */
public final class SharedPostgresContainer {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:14-alpine");

    private static PostgreSQLContainer<?> container;

    private SharedPostgresContainer() {
    }

    public static synchronized PostgreSQLContainer<?> getInstance() {
        if (container == null) {
            container = new PostgreSQLContainer<>(IMAGE)
                    .withDatabaseName("upyog_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);
            container.start();
        }
        return container;
    }
}
