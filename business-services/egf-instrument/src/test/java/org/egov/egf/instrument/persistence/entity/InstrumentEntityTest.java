package org.egov.egf.instrument.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentEntityTest {
    @Test
    void classLoads() {
        assertThat(InstrumentEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
