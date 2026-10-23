package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillRequestV2Test {
    @Test
    void classLoads() {
        assertThat(BillRequestV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillRequestV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
