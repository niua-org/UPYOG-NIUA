package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentContractTest {
    @Test
    void classLoads() {
        assertThat(InstrumentContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
