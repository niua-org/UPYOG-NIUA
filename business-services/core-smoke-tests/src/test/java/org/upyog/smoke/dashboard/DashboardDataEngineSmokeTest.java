package org.upyog.smoke.dashboard;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class DashboardDataEngineSmokeTest extends ServiceSmokeTestTemplate {

    public DashboardDataEngineSmokeTest() {
        super("dashboard-data-engine");
    }
}
