package org.egov.core.smoke.support;

import java.util.Locale;
import java.util.Optional;

/**
 * Environment-only configuration. No URLs or secrets are compiled into this class.
 * Required: ENVIRONMENT, BASE_URL, SMOKE_TENANT_ID, and either SMOKE_TOKEN or
 * SMOKE_USER + SMOKE_PASSWORD. Optional: EXPECTED_VERSION, SERVICE (default all).
 */
public final class SmokeConfig {

    public static final String ALL_SERVICES = "all";

    private final String environment;
    private final String baseUrl;
    private final String tenantId;
    private final String token;
    private final String user;
    private final String password;
    private final String expectedVersion;
    private final String service;

    private SmokeConfig(
            String environment,
            String baseUrl,
            String tenantId,
            String token,
            String user,
            String password,
            String expectedVersion,
            String service) {
        this.environment = environment;
        this.baseUrl = baseUrl;
        this.tenantId = tenantId;
        this.token = token;
        this.user = user;
        this.password = password;
        this.expectedVersion = expectedVersion;
        this.service = service;
    }

    public static SmokeConfig load() {
        String environment = required("ENVIRONMENT");
        String baseUrl = stripTrailingSlash(required("BASE_URL"));
        String tenantId = required("SMOKE_TENANT_ID");
        String token = optional("SMOKE_TOKEN").orElse("");
        String user = optional("SMOKE_USER").orElse("");
        String password = optional("SMOKE_PASSWORD").orElse("");
        if (token.isBlank() && (user.isBlank() || password.isBlank())) {
            throw new IllegalStateException(
                    "Missing credentials: set SMOKE_TOKEN, or both SMOKE_USER and SMOKE_PASSWORD");
        }
        String expectedVersion = optional("EXPECTED_VERSION").filter(v -> !v.isBlank()).orElse(null);
        String service = optional("SERVICE")
                .or(() -> optionalProperty("service"))
                .filter(v -> !v.isBlank())
                .orElse(ALL_SERVICES);
        return new SmokeConfig(environment, baseUrl, tenantId, token, user, password, expectedVersion, service);
    }

    public String environment() {
        return environment;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String tenantId() {
        return tenantId;
    }

    public Optional<String> token() {
        return token.isBlank() ? Optional.empty() : Optional.of(token);
    }

    public Optional<String> user() {
        return user.isBlank() ? Optional.empty() : Optional.of(user);
    }

    public Optional<String> password() {
        return password.isBlank() ? Optional.empty() : Optional.of(password);
    }

    public boolean hasStaticToken() {
        return token().isPresent();
    }

    public Optional<String> expectedVersion() {
        return Optional.ofNullable(expectedVersion).filter(v -> !v.isBlank());
    }

    public String service() {
        return service;
    }

    public boolean matchesService(String serviceName) {
        return ALL_SERVICES.equalsIgnoreCase(service) || service.equalsIgnoreCase(serviceName);
    }

    private static String required(String name) {
        String value = read(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value.trim();
    }

    private static Optional<String> optional(String name) {
        String value = read(name);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private static Optional<String> optionalProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private static String read(String name) {
        String fromEnv = System.getenv(name);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return System.getProperty(name);
    }

    private static String stripTrailingSlash(String url) {
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static String normalizeService(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL_SERVICES;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
