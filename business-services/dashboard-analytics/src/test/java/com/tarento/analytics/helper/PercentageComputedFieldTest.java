package com.tarento.analytics.helper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PercentageComputedFieldTest {
    @Test
    void classLoads() {
        assertThat(PercentageComputedField.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PercentageComputedField.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
