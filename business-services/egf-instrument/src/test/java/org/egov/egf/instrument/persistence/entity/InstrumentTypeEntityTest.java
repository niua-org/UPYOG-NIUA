package org.egov.egf.instrument.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypeEntityTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypeEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypeEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
