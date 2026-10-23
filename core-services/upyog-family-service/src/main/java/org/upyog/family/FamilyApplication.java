package org.upyog.family;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot Application Entry Point for upyog-family-service.
 *
 * <p>Central registry service for household units, family linkages, and citizen relations under UPYOG.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"org.upyog.family"})
public class FamilyApplication {

    /**
     * Main method bootstrap launching the Spring Boot microservice runtime for UPYOG Family Service.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(FamilyApplication.class, args);
    }
}
