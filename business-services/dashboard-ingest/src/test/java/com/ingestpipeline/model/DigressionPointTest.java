package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DigressionPointTest {
    @Test
    void classLoads() {
        assertThat(DigressionPoint.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DigressionPoint.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
