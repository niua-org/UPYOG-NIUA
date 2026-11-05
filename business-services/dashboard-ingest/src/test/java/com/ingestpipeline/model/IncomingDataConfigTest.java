package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IncomingDataConfigTest {
    @Test
    void classLoads() {
        assertThat(IncomingDataConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IncomingDataConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
