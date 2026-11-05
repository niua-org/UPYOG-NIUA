package org.niua.smoke.tpservices;

import org.niua.smoke.support.AbstractServiceSmokeTest;
import org.niua.smoke.support.ServiceDefinition;
import org.niua.smoke.support.ServiceRegistry;
import org.niua.smoke.support.SmokeService;

@SmokeService("tp-services")
public class TpServicesSmokeTest extends AbstractServiceSmokeTest {

    @Override
    protected ServiceDefinition serviceDefinition() {
        return ServiceRegistry.require("tp-services");
    }
}
