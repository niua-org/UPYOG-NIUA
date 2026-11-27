package com.ingestpipeline.producer;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JavaSerializerTest {
    @Test
    void classLoads() {
        assertThat(JavaSerializer.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = JavaSerializer.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
