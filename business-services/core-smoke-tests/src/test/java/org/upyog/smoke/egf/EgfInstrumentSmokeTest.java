package org.upyog.smoke.egf;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class EgfInstrumentSmokeTest extends ServiceSmokeTestTemplate {

    public EgfInstrumentSmokeTest() {
        super("egf-instrument");
    }
}
