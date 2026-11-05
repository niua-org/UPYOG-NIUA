package org.niua.smoke.challangeneration;

import org.niua.smoke.support.AbstractServiceSmokeTest;
import org.niua.smoke.support.ServiceDefinition;
import org.niua.smoke.support.ServiceRegistry;
import org.niua.smoke.support.SmokeService;

@SmokeService("challan-generation")
public class ChallanGenerationSmokeTest extends AbstractServiceSmokeTest {

    @Override
    protected ServiceDefinition serviceDefinition() {
        return ServiceRegistry.require("challan-generation");
    }
}
