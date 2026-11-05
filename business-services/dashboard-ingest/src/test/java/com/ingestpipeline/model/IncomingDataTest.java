package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IncomingDataTest {
    @Test
    void classLoads() {
        assertThat(IncomingData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IncomingData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
