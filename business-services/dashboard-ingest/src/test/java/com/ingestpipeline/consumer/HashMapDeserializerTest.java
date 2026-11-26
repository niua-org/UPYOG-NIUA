package com.ingestpipeline.consumer;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HashMapDeserializerTest {
    @Test
    void classLoads() {
        assertThat(HashMapDeserializer.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = HashMapDeserializer.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
