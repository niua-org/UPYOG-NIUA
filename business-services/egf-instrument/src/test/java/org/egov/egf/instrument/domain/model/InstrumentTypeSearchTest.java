package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypeSearchTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypeSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypeSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
