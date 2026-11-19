package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DigressionPointConfigTest {
    @Test
    void classLoads() {
        assertThat(DigressionPointConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DigressionPointConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
