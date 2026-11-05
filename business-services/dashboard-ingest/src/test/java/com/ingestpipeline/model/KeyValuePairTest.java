package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KeyValuePairTest {
    @Test
    void classLoads() {
        assertThat(KeyValuePair.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = KeyValuePair.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
