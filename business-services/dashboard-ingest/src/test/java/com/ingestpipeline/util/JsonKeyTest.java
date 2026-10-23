package com.ingestpipeline.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JsonKeyTest {
    @Test
    void classLoads() {
        assertThat(JsonKey.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = JsonKey.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
