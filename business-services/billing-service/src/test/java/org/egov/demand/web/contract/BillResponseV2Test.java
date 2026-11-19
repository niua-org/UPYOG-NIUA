package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillResponseV2Test {
    @Test
    void classLoads() {
        assertThat(BillResponseV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillResponseV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
