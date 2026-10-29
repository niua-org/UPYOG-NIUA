package org.upyog.smoke.verification;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class VerificationServiceSmokeTest extends ServiceSmokeTestTemplate {

    public VerificationServiceSmokeTest() {
        super("verification-service");
    }
}
