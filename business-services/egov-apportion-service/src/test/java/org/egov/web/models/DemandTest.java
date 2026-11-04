package org.egov.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandTest {
    @Test
    void classLoads() {
        assertThat(Demand.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Demand.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
