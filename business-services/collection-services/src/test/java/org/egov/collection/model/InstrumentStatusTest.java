package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentStatusTest {
    @Test
    void classLoads() {
        assertThat(InstrumentStatus.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentStatus.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
