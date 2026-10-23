package org.upyog.smoke.support;

import java.util.HashMap;
import java.util.Map;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Map<String, Object> minimalRequestInfo() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("uuid", "SMOKE_USER_UUID");
        userInfo.put("type", "EMPLOYEE");

        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("apiId", "SMOKE");
        requestInfo.put("ver", "1.0");
        requestInfo.put("ts", System.currentTimeMillis());
        requestInfo.put("action", "search");
        requestInfo.put("userInfo", userInfo);
        return requestInfo;
    }

    public static String smokePrefix() {
        return "SMOKE_";
    }
}
