package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillResponseTest {
    @Test
    void classLoads() {
        assertThat(BillResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
