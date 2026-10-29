package org.niua.smoke.support;

public final class TenantContext {

    private TenantContext() {
    }

    public static String smokeTenant() {
        return SmokeConfig.smokeTenantId();
    }

    public static String isolationTenant() {
        return System.getenv().getOrDefault("SMOKE_ISOLATION_TENANT_ID", smokeTenant() + ".isolation");
    }

    public static String smokeDataPrefix() {
        return "SMOKE_";
    }
}
