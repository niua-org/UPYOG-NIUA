package org.upyog.smoke.support;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ServiceRegistry {

    private ServiceRegistry() {
    }

    @SuppressWarnings("unchecked")
    public static List<ServiceDefinition> loadFiltered() {
        Yaml yaml = new Yaml();
        try (InputStream in = ServiceRegistry.class.getClassLoader().getResourceAsStream("services.yaml")) {
            if (in == null) {
                throw new IllegalStateException("services.yaml not found on classpath");
            }
            Map<String, Object> root = yaml.load(in);
            List<Map<String, Object>> raw = (List<Map<String, Object>>) root.get("services");
            List<ServiceDefinition> all = raw.stream().map(ServiceRegistry::map).collect(Collectors.toList());
            String filter = SmokeConfig.serviceFilter();
            if ("ALL".equals(filter)) {
                return all;
            }
            return all.stream()
                    .filter(s -> s.getName().toUpperCase(Locale.ROOT).equals(filter)
                            || s.getName().replace('-', '_').toUpperCase(Locale.ROOT).equals(filter))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load services.yaml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static ServiceDefinition map(Map<String, Object> raw) {
        ServiceDefinition def = new ServiceDefinition();
        def.setName((String) raw.get("name"));
        def.setBasePath((String) raw.get("basePath"));
        def.setHealthPath((String) raw.get("healthPath"));
        def.setVersionPath((String) raw.get("versionPath"));
        def.setDependsOn((List<String>) raw.get("dependsOn"));
        def.setCoreRead((Map<String, Object>) raw.get("coreRead"));
        def.setCoreWrite((Map<String, Object>) raw.get("coreWrite"));
        return def;
    }
}
