package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConsumableTest {
    @Test
    void classLoads() {
        assertThat(Consumable.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Consumable.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
