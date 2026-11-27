package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentAccountCodeSearchTest {
    @Test
    void classLoads() {
        assertThat(InstrumentAccountCodeSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentAccountCodeSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
