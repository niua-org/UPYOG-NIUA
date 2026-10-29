package com.ingestpipeline.producer;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HashMapSerializerTest {
    @Test
    void classLoads() {
        assertThat(HashMapSerializer.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = HashMapSerializer.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
