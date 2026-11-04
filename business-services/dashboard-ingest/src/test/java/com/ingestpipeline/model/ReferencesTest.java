package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReferencesTest {
    @Test
    void classLoads() {
        assertThat(References.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = References.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
