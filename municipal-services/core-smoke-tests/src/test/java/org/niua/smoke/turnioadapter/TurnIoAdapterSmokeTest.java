package org.niua.smoke.turnioadapter;

import org.niua.smoke.support.AbstractServiceSmokeTest;
import org.niua.smoke.support.ServiceDefinition;
import org.niua.smoke.support.ServiceRegistry;
import org.niua.smoke.support.SmokeService;

@SmokeService("turn-io-adapter")
public class TurnIoAdapterSmokeTest extends AbstractServiceSmokeTest {

    @Override
    protected ServiceDefinition serviceDefinition() {
        return ServiceRegistry.require("turn-io-adapter");
    }
}
