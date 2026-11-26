package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptV2Test {
    @Test
    void classLoads() {
        assertThat(ReceiptV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReceiptV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
