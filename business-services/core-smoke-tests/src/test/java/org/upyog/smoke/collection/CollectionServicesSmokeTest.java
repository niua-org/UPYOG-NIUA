package org.upyog.smoke.collection;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class CollectionServicesSmokeTest extends ServiceSmokeTestTemplate {

    public CollectionServicesSmokeTest() {
        super("collection-services");
    }
}
