package org.egov.core.smoke.support;

/**
 * Markers for smoke data. Payloads stay on the smoke tenant and use a {@code SMOKE_} prefix.
 * No URLs or secrets are stored here.
 */
public final class TestDataFactory {

    private final TenantContext tenants;

    public TestDataFactory(TenantContext tenants) {
        this.tenants = tenants;
    }

    public static TestDataFactory from(SmokeConfig config) {
        return new TestDataFactory(TenantContext.from(config));
    }

    public String tenantId() {
        return tenants.smokeTenant();
    }

    public String marker() {
        return "SMOKE_" + System.currentTimeMillis();
    }
}
