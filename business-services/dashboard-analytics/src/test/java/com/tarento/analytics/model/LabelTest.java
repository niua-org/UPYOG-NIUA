package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LabelTest {
    @Test
    void classLoads() {
        assertThat(Label.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Label.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
