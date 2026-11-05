package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentGlCodeMappingTest {
    @Test
    void classLoads() {
        assertThat(InstrumentGlCodeMapping.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentGlCodeMapping.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
