package org.upyog.smoke.dashboard;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class DashboardIngestSmokeTest extends ServiceSmokeTestTemplate {

    public DashboardIngestSmokeTest() {
        super("dashboard-ingest");
    }
}
