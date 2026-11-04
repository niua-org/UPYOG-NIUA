package org.upyog.smoke.apportion;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class EgovApportionServiceSmokeTest extends ServiceSmokeTestTemplate {

    public EgovApportionServiceSmokeTest() {
        super("egov-apportion-service");
    }
}
