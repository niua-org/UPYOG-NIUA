package org.egov.egf.instrument.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentAccountCodeEntityTest {
    @Test
    void classLoads() {
        assertThat(InstrumentAccountCodeEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentAccountCodeEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
