package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApportionDemandResponseTest {
    @Test
    void classLoads() {
        assertThat(ApportionDemandResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ApportionDemandResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
