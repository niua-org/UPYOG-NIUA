package org.upyog.smoke.support;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

public final class SmokeConfig {

    public static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration HEALTH_POLL_TIMEOUT = Duration.ofSeconds(90);
    public static final Duration HEALTH_POLL_INTERVAL = Duration.ofSeconds(3);

    private SmokeConfig() {
    }

    public static String require(String sysProp, String envName) {
        String fromSys = System.getProperty(sysProp);
        if (fromSys != null && !fromSys.isBlank()) {
            return fromSys.trim();
        }
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        throw new IllegalStateException("Missing required config: set -D" + sysProp + " or env " + envName);
    }

    public static String optional(String sysProp, String envName, String defaultValue) {
        return Optional.ofNullable(System.getProperty(sysProp))
                .filter(s -> !s.isBlank())
                .or(() -> Optional.ofNullable(System.getenv(envName)).filter(s -> !s.isBlank()))
                .orElse(defaultValue);
    }

    public static String baseUrl() {
        return require("base.url", "BASE_URL").replaceAll("/$", "");
    }

    public static String environment() {
        return require("environment", "ENVIRONMENT");
    }

    public static String smokeTenantId() {
        return require("smoke.tenant.id", "SMOKE_TENANT_ID");
    }

    public static String serviceFilter() {
        return optional("service", "SERVICE", "ALL").toUpperCase(Locale.ROOT);
    }

    public static Optional<String> expectedVersion() {
        String v = optional("expected.version", "EXPECTED_VERSION", "");
        return v.isBlank() ? Optional.empty() : Optional.of(v);
    }

    public static String maskSecret(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
