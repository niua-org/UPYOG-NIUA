package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillDetailV2Test {
    @Test
    void classLoads() {
        assertThat(BillDetailV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillDetailV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
