package org.upyog.smoke.dashboard;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class DashboardDataExtractorSmokeTest extends ServiceSmokeTestTemplate {

    public DashboardDataExtractorSmokeTest() {
        super("dashboard-data-extractor");
    }
}
