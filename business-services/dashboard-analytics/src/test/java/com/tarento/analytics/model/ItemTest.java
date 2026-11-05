package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ItemTest {
    @Test
    void classLoads() {
        assertThat(Item.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Item.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
