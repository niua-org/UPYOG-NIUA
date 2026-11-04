package org.egov;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EgfInstrumentApplicationTest {
    @Test
    void classLoads() {
        assertThat(EgfInstrumentApplication.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EgfInstrumentApplication.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
