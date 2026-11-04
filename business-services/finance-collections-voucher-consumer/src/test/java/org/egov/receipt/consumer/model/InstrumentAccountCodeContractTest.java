package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentAccountCodeContractTest {
    @Test
    void classLoads() {
        assertThat(InstrumentAccountCodeContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentAccountCodeContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
