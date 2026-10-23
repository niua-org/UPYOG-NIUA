package org.niua.smoke.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal RequestInfo / tenant wrappers for smoke payloads (structure only).
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Map<String, Object> requestInfo(String tenantId) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("uuid", UUID.randomUUID().toString());
        userInfo.put("tenantId", tenantId);
        userInfo.put("type", "EMPLOYEE");
        userInfo.put("roles", List.of(Map.of("code", "EMPLOYEE", "tenantId", tenantId)));

        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("apiId", "core-smoke-tests");
        requestInfo.put("ver", "1.0");
        requestInfo.put("ts", System.currentTimeMillis());
        requestInfo.put("action", "create");
        requestInfo.put("msgId", TenantContext.smokeDataPrefix() + UUID.randomUUID());
        requestInfo.put("authToken", "smoke");
        requestInfo.put("userInfo", userInfo);
        return requestInfo;
    }

    public static Map<String, Object> requestInfoWrapper(String tenantId) {
        return Map.of("RequestInfo", requestInfo(tenantId));
    }
}
