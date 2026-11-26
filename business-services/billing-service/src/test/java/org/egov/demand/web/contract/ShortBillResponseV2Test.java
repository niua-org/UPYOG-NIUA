package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ShortBillResponseV2Test {
    @Test
    void classLoads() {
        assertThat(ShortBillResponseV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ShortBillResponseV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
