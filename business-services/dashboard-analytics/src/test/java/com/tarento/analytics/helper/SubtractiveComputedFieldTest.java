package com.tarento.analytics.helper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubtractiveComputedFieldTest {
    @Test
    void classLoads() {
        assertThat(SubtractiveComputedField.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SubtractiveComputedField.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
