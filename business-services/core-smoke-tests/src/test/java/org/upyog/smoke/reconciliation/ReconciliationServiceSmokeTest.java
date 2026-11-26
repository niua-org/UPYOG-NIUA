package org.upyog.smoke.reconciliation;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class ReconciliationServiceSmokeTest extends ServiceSmokeTestTemplate {

    public ReconciliationServiceSmokeTest() {
        super("reconciliation-service");
    }
}
