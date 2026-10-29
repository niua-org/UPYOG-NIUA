package org.upyog.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.upyog.mcp.config.McpProperties;

/**
 * Entry point for the UPYOG MCP server.
 * <p>
 * The process is a stateless orchestration layer. It exposes a fixed set of MCP tools
 * and calls existing UPYOG services through the API gateway. It does not run a model.
 */
@SpringBootApplication
@EnableConfigurationProperties(McpProperties.class)
public class McpServerApplication {

    /**
     * Bootstraps Spring Boot, loads descriptors, and starts Streamable HTTP MCP on {@code /mcp}.
     *
     * @param args standard Spring Boot command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
