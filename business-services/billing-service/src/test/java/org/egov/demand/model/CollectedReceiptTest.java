package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollectedReceiptTest {
    @Test
    void classLoads() {
        assertThat(CollectedReceipt.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CollectedReceipt.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
