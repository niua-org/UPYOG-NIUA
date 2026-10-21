package org.upyog.mcp.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.upyog.mcp.config.McpProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads descriptor YAML at startup. An invalid file aborts the process.
 */
@Component
public class DescriptorLoader {

    private final DescriptorRegistry registry;
    private final McpProperties properties;
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public DescriptorLoader(DescriptorRegistry registry, McpProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    /** Loads and validates descriptors before the server accepts MCP traffic. */
    @Bean
    ApplicationRunner loadDescriptors() {
        return args -> registry.replace(read("classpath:descriptors/*.yaml"));
    }

    /**
     * Reads every YAML file matching {@code pattern}, validates, and returns parsed services.
     *
     * @param pattern Spring resource pattern (for example {@code classpath:descriptors/*.yaml})
     */
    public List<ServiceDescriptor> read(String pattern) throws IOException {
        DescriptorValidator validator = new DescriptorValidator(properties);
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(pattern);
        if (resources.length == 0) {
            throw new IllegalStateException("No descriptors found at " + pattern);
        }
        List<ServiceDescriptor> loaded = new ArrayList<>();
        for (Resource resource : resources) {
            try (InputStream input = resource.getInputStream()) {
                ServiceDescriptor service = yaml.readValue(input, ServiceDescriptor.class);
                service.getOperations().forEach((name, operation) -> operation.setName(name));
                validator.validate(service);
                loaded.add(service);
            }
        }
        return loaded;
    }
}
