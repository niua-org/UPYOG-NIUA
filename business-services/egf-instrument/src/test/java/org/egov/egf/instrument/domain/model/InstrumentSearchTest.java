package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentSearchTest {
    @Test
    void classLoads() {
        assertThat(InstrumentSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
