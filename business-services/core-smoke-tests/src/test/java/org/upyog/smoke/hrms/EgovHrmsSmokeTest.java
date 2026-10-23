package org.upyog.smoke.hrms;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class EgovHrmsSmokeTest extends ServiceSmokeTestTemplate {

    public EgovHrmsSmokeTest() {
        super("egov-hrms");
    }
}
