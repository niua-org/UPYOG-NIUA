package org.egov.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger configuration for the eGov User Service.
 *
 * <p>Exposes interactive API documentation and metadata for all REST endpoints,
 * including citizen profile management, multi-channel authentication (V2),
 * address lifecycle management, and citizen document verification.</p>
 *
 * @author UPYOG Team
 * @since 2.9.0
 */
@Configuration
public class SwaggerConfiguration {

    /**
     * Creates and configures the {@link OpenAPI} metadata bean.
     *
     * @return initialized {@link OpenAPI} specification bean
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("eGov User Service API")
                        .version("2.9.0")
                        .description("API documentation for eGov User Management, Multi-Auth (V2), Citizen Profile, Address Management, and Document Verification services.")
                        .contact(new Contact()
                                .name("UPYOG NIUA Team")
                                .email("contact@upyog.niua.org")
                                .url("https://upyog.niua.org"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
