package org.niua.smoke.support;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * All configuration from environment variables — no hardcoded URLs or secrets.
 */
public final class SmokeConfig {

    private SmokeConfig() {
    }

    public static String environment() {
        return require("ENVIRONMENT");
    }

    public static String baseUrl() {
        return stripTrailingSlash(require("BASE_URL"));
    }

    public static String smokeTenantId() {
        return require("SMOKE_TENANT_ID");
    }

    public static String smokeUser() {
        return require("SMOKE_USER");
    }

    public static String smokePassword() {
        return require("SMOKE_PASSWORD");
    }

    public static Optional<String> expectedVersion() {
        return optional("EXPECTED_VERSION");
    }

    public static String serviceFilter() {
        return System.getProperty("service", optional("SERVICE").orElse("ALL")).trim();
    }

    public static Duration httpTimeout() {
        return Duration.ofSeconds(Long.parseLong(optional("SMOKE_HTTP_TIMEOUT_SECONDS").orElse("30")));
    }

    public static Duration healthWarmUp() {
        return Duration.ofSeconds(Long.parseLong(optional("SMOKE_HEALTH_WARMUP_SECONDS").orElse("90")));
    }

    public static Optional<String> authTokenOverride() {
        return optional("SMOKE_AUTH_TOKEN");
    }

    private static String require(String key) {
        return optional(key).orElseThrow(() ->
                new IllegalStateException("Missing required smoke test env var: " + key
                        + ". See municipal-services/core-smoke-tests/README.md"));
    }

    private static Optional<String> optional(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(v.trim());
    }

    private static String stripTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    public static String maskSecret(String value) {
        if (value == null || value.length() < 8) {
            return "***";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 2);
    }

    public static String normalizedServiceName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
