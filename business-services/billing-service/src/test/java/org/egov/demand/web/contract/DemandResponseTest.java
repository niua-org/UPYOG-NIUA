package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandResponseTest {
    @Test
    void classLoads() {
        assertThat(DemandResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DemandResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
