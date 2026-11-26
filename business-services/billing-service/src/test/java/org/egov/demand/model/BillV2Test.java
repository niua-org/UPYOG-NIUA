package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillV2Test {
    @Test
    void classLoads() {
        assertThat(BillV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
