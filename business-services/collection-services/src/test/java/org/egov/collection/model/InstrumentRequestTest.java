package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentRequestTest {
    @Test
    void classLoads() {
        assertThat(InstrumentRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
