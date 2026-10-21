package org.upyog.smoke.e2e;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.upyog.smoke.support.ServiceFilter;
import org.upyog.smoke.support.SmokeConfig;

@Tag("smoke")
public class BillingToCollectionE2ESmokeTest {

    @Test
    void crossService_journey_placeholder_documentedInReadme() {
        Assumptions.assumeTrue("ALL".equals(SmokeConfig.serviceFilter()),
                "Cross-service E2E runs only when SERVICE=ALL");
        Assumptions.assumeTrue(false,
                "Implement tenant-scoped demand search -> payment search journey when smoke tenant seed is available");
    }
}
