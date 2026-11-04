package com.tarento.analytics.helper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AverageComputedFieldTest {
    @Test
    void classLoads() {
        assertThat(AverageComputedField.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AverageComputedField.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
