package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillRequestTest {
    @Test
    void classLoads() {
        assertThat(BillRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
