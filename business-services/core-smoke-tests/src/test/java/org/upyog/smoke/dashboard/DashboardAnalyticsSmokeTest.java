package org.upyog.smoke.dashboard;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class DashboardAnalyticsSmokeTest extends ServiceSmokeTestTemplate {

    public DashboardAnalyticsSmokeTest() {
        super("dashboard-analytics");
    }
}
