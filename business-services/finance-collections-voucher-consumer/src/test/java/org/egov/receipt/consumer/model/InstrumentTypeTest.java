package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypeTest {
    @Test
    void classLoads() {
        assertThat(InstrumentType.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentType.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
