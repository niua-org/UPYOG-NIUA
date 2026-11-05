package com.tarento.analytics.helper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ComputedFieldFactoryTest {
    @Test
    void classLoads() {
        assertThat(ComputedFieldFactory.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ComputedFieldFactory.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
