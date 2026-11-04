package org.niua.smoke.property;

import org.niua.smoke.support.AbstractServiceSmokeTest;
import org.niua.smoke.support.ServiceDefinition;
import org.niua.smoke.support.ServiceRegistry;
import org.niua.smoke.support.SmokeService;

@SmokeService("property-services")
public class PropertyServicesSmokeTest extends AbstractServiceSmokeTest {

    @Override
    protected ServiceDefinition serviceDefinition() {
        return ServiceRegistry.require("property-services");
    }
}
