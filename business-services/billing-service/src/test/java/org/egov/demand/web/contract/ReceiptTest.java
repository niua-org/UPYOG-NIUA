package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptTest {
    @Test
    void classLoads() {
        assertThat(Receipt.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Receipt.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
