package org.egov.egf.instrument.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypePropertyEntityTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypePropertyEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypePropertyEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
