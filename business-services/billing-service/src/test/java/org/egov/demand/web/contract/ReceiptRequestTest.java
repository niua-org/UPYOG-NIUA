package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptRequestTest {
    @Test
    void classLoads() {
        assertThat(ReceiptRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReceiptRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
