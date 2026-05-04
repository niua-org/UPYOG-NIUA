package org.egov.infra.config.persistence.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

@Configuration
public class DBMigrationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBMigrationConfiguration.class);

    @Value("${dev.mode}")
    private boolean devMode;

    @Value("${db.migration.enabled}")
    private boolean dbMigrationEnabled;

    @Value("${db.flyway.validateon.migrate}")
    private boolean validateOnMigrate;

    @Value("${db.flyway.migration.repair}")
    private boolean repairMigration;

    @Value("${statewide.migration.required}")
    private boolean statewideMigrationRequired;

    @Value("${db.flyway.main.migration.file.path}")
    private String mainMigrationFilePath;

    @Value("${db.flyway.sample.migration.file.path}")
    private String sampleMigrationFilePath;

    @Value("${db.flyway.tenant.migration.file.path}")
    private String tenantMigrationFilePath;

    @Value("${db.flyway.statewide.migration.file.path}")
    private String statewideMigrationFilePath;

    @Value("${statewide.schema.name}")
    private String statewideSchemaName;

    @Autowired
    private ConfigurableEnvironment environment;

    @Bean
    @DependsOn("dataSource")
    public Flyway flyway(DataSource dataSource, @Qualifier("cities") List<String> cities) {
        if (dbMigrationEnabled) {
            cities.stream().forEach(schema -> {
                if (devMode)
                    migrateDatabase(dataSource, schema,
                            mainMigrationFilePath, sampleMigrationFilePath, format(tenantMigrationFilePath, schema));
                else
                    migrateDatabase(dataSource, schema,
                            mainMigrationFilePath, format(tenantMigrationFilePath, schema));
            });

            if (statewideMigrationRequired && !devMode) {
                migrateDatabase(dataSource, statewideSchemaName, mainMigrationFilePath, statewideMigrationFilePath);
            } else if (!devMode) {
                migrateDatabase(dataSource, statewideSchemaName, mainMigrationFilePath);
            }
        }

        return null;
    }

    private void migrateDatabase(DataSource dataSource, String schema, String... locations) {
    	// Use Flyway.configure() to create a Flyway instanceAdd commentMore actions
        FluentConfiguration flywayConfig = Flyway.configure()
            .dataSource(dataSource)
            .locations(locations)
            .schemas(schema)
            .baselineOnMigrate(true)
            .validateOnMigrate(validateOnMigrate)
            .outOfOrder(true);

        Flyway flyway = flywayConfig.load();

        if (repairMigration) {
            flyway.repair();
        }
        flyway.migrate();
    }

    @Bean(name = "tenants", autowire = Autowire.BY_NAME)
    public List<String> tenants() {
        List<String> tenants = new ArrayList<>();

        String configuredSchemas = environment.getProperty("tenant.schemas");
        if (configuredSchemas != null && !configuredSchemas.trim().isEmpty()) {
            LOGGER.info("======== LOADING TENANT SCHEMAS FROM tenant.schemas ========");
            Arrays.stream(configuredSchemas.split(","))
                    .map(String::trim)
                    .filter(schema -> !schema.isEmpty())
                    .forEach(tenants::add);
            LOGGER.info("Final tenants list: {}", tenants);
            LOGGER.info("======== TENANT SCAN COMPLETE ========");
            return tenants;
        }

        LOGGER.info("======== SCANNING PROPERTY SOURCES FOR TENANT KEYS ========");

        environment.getPropertySources().iterator().forEachRemaining(propertySource -> {
            LOGGER.info("Scanning PropertySource: [{}] of type: [{}]",
                    propertySource.getName(), propertySource.getClass().getName());

            if (propertySource instanceof MapPropertySource) {
                ((MapPropertySource) propertySource).getSource().forEach((key, value) -> {
                    if (key.startsWith("tenant.")) {
                        LOGGER.info("Found tenant key: [{}] = [{}] in PropertySource: [{}]",
                                key, value, propertySource.getName());
                        tenants.add(value.toString());
                    }
                });
            }
        });

        LOGGER.info("Final tenants list: {}", tenants);
        LOGGER.info("======== TENANT SCAN COMPLETE ========");

        return tenants;
    }

}
