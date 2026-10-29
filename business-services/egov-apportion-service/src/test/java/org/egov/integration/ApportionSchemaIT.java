package org.egov.integration;

import org.egov.ApportionApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.upyog.testsupport.containers.SharedPostgresContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApportionApp.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ApportionSchemaIT {

    @Container
    static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getInstance();

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigration_createsApportionTables() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'eg_appr_bills_request'",
                Integer.class);

        assertThat(tableCount).isEqualTo(1);
    }
}
