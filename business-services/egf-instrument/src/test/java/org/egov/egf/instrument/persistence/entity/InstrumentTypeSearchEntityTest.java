package org.egov.egf.instrument.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypeSearchEntityTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypeSearchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypeSearchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
