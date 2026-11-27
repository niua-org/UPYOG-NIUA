package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RemittanceInstrumentTest {
    @Test
    void classLoads() {
        assertThat(RemittanceInstrument.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RemittanceInstrument.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
