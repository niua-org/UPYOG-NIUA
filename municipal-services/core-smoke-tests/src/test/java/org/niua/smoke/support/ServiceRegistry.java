package org.niua.smoke.support;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ServiceRegistry {

    private ServiceRegistry() {
    }

    public static List<ServiceDefinition> loadAll() {
        Yaml yaml = new Yaml();
        try (InputStream in = ServiceRegistry.class.getResourceAsStream("/services.yaml")) {
            if (in == null) {
                throw new IllegalStateException("services.yaml not found on classpath");
            }
            Map<String, Object> root = yaml.load(in);
            List<Map<String, Object>> services = (List<Map<String, Object>>) root.get("services");
            return services.stream().map(ServiceDefinition::fromMap).collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load services.yaml", e);
        }
    }

    public static ServiceDefinition require(String serviceName) {
        return loadAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(serviceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown service in services.yaml: " + serviceName));
    }
}
