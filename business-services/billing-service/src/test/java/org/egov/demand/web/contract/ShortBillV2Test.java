package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ShortBillV2Test {
    @Test
    void classLoads() {
        assertThat(ShortBillV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ShortBillV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
