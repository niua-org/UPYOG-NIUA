package org.egov.core.smoke.support;

/**
 * Tenant ids for smoke calls. The smoke tenant comes only from {@code SMOKE_TENANT_ID}.
 * {@link #otherTenant()} is a synthetic id used solely when a tenant-scoped path is confirmed.
 */
public final class TenantContext {

    private final String smokeTenantId;

    private TenantContext(String smokeTenantId) {
        this.smokeTenantId = smokeTenantId;
    }

    public static TenantContext from(SmokeConfig config) {
        return new TenantContext(config.tenantId());
    }

    public String smokeTenant() {
        return smokeTenantId;
    }

    public String otherTenant() {
        return "SMOKE_ISOLATION";
    }
}
