package org.egov.egf.instrument.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentAccountCodeSearchEntityTest {
    @Test
    void classLoads() {
        assertThat(InstrumentAccountCodeSearchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentAccountCodeSearchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
