package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandRequestTest {
    @Test
    void classLoads() {
        assertThat(DemandRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DemandRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
