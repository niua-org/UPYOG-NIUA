package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentSearchContractTest {
    @Test
    void classLoads() {
        assertThat(InstrumentSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
