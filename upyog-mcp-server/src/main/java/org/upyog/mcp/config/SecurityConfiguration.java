package org.upyog.mcp.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Refuses to start when the HMAC secret is missing or shorter than 32 characters.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    ApplicationRunner requireTokenSecret(McpProperties properties) {
        return args -> {
            if (properties.getTokenSecret() == null || properties.getTokenSecret().length() < 32) {
                throw new IllegalStateException("UPYOG_MCP_TOKEN_SECRET must be at least 32 characters");
            }
        };
    }
}
