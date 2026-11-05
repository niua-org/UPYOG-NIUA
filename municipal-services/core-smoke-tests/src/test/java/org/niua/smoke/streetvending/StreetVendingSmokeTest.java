package org.niua.smoke.streetvending;

import org.niua.smoke.support.AbstractServiceSmokeTest;
import org.niua.smoke.support.ServiceDefinition;
import org.niua.smoke.support.ServiceRegistry;
import org.niua.smoke.support.SmokeService;

@SmokeService("street-vending")
public class StreetVendingSmokeTest extends AbstractServiceSmokeTest {

    @Override
    protected ServiceDefinition serviceDefinition() {
        return ServiceRegistry.require("street-vending");
    }
}
