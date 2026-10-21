package org.upyog.smoke.support;

import java.util.Locale;

public final class ServiceFilter {

    private ServiceFilter() {
    }

    public static boolean matches(String serviceName) {
        String filter = SmokeConfig.serviceFilter();
        if ("ALL".equals(filter)) {
            return true;
        }
        String normalized = serviceName.toUpperCase(Locale.ROOT).replace('-', '_');
        return normalized.equals(filter) || serviceName.equalsIgnoreCase(filter);
    }
}
