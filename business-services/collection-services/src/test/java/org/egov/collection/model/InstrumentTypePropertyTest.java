package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypePropertyTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypeProperty.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypeProperty.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
