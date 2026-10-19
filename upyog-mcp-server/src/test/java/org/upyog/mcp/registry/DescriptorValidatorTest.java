package org.upyog.mcp.registry;

import org.junit.jupiter.api.Test;
import org.upyog.mcp.config.McpProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DescriptorValidatorTest {

    @Test
    void loadsPackagedDescriptors() throws Exception {
        McpProperties properties = properties();
        List<ServiceDescriptor> services = new DescriptorLoader(new DescriptorRegistry(), properties)
                .read("classpath:descriptors/*.yaml");
        assertEquals(4, services.size());
    }

    @Test
    void rejectsArbitraryHttpMethod() {
        McpProperties properties = properties();
        DescriptorLoader loader = new DescriptorLoader(new DescriptorRegistry(), properties);
        assertThrows(IllegalStateException.class,
                () -> loader.read("classpath:bad-descriptors/invalid-method.yaml"));
    }

    private static McpProperties properties() {
        McpProperties properties = new McpProperties();
        properties.setAllowedGatewayPrefixes(List.of("/pgr-services/", "/property-services/",
                "/adv-services/", "/chb-services/", "/billing-service/", "/egov-mdms-service/", "/filestore/"));
        return properties;
    }
}
