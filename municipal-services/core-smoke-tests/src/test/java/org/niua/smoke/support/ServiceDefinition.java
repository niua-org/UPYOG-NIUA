package org.niua.smoke.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ServiceDefinition {

    private final String name;
    private final String basePath;
    private final String healthPath;
    private final String versionPath;
    private final List<String> dependsOn;
    private final Optional<Endpoint> coreRead;
    private final Optional<Endpoint> coreWrite;

    public ServiceDefinition(
            String name,
            String basePath,
            String healthPath,
            String versionPath,
            List<String> dependsOn,
            Optional<Endpoint> coreRead,
            Optional<Endpoint> coreWrite) {
        this.name = name;
        this.basePath = basePath;
        this.healthPath = healthPath;
        this.versionPath = versionPath;
        this.dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
        this.coreRead = coreRead;
        this.coreWrite = coreWrite;
    }

    @SuppressWarnings("unchecked")
    public static ServiceDefinition fromMap(Map<String, Object> raw) {
        String name = (String) raw.get("name");
        String basePath = (String) raw.get("basePath");
        String healthPath = (String) raw.getOrDefault("healthPath", "/health");
        String versionPath = (String) raw.getOrDefault("versionPath", "/info");
        List<String> dependsOn = raw.get("dependsOn") instanceof List<?> list
                ? (List<String>) list
                : Collections.emptyList();
        Optional<Endpoint> read = endpointFrom(raw.get("coreRead"));
        Optional<Endpoint> write = endpointFrom(raw.get("coreWrite"));
        return new ServiceDefinition(name, basePath, healthPath, versionPath, dependsOn, read, write);
    }

    private static Optional<Endpoint> endpointFrom(Object node) {
        if (!(node instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        String method = (String) map.get("method");
        String path = (String) map.get("path");
        if (method == null || path == null) {
            return Optional.empty();
        }
        return Optional.of(new Endpoint(method, path));
    }

    public String getName() {
        return name;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public String getVersionPath() {
        return versionPath;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public Optional<Endpoint> getCoreRead() {
        return coreRead;
    }

    public Optional<Endpoint> getCoreWrite() {
        return coreWrite;
    }

    public String serviceUrl(String baseUrl) {
        return baseUrl + basePath;
    }

    public record Endpoint(String method, String path) {
    }
}
