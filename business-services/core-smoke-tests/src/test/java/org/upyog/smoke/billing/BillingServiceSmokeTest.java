package org.upyog.smoke.billing;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class BillingServiceSmokeTest extends ServiceSmokeTestTemplate {

    public BillingServiceSmokeTest() {
        super("billing-service");
    }
}
