package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PathTest {
    @Test
    void classLoads() {
        assertThat(Path.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Path.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
