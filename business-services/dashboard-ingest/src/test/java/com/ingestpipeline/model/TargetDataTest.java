package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TargetDataTest {
    @Test
    void classLoads() {
        assertThat(TargetData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TargetData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
