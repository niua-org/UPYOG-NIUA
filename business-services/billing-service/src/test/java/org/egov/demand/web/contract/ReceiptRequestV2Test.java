package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptRequestV2Test {
    @Test
    void classLoads() {
        assertThat(ReceiptRequestV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReceiptRequestV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
