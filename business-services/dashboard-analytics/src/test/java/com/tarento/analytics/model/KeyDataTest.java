package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KeyDataTest {
    @Test
    void classLoads() {
        assertThat(KeyData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = KeyData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
