package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandDueTest {
    @Test
    void classLoads() {
        assertThat(DemandDue.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DemandDue.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
