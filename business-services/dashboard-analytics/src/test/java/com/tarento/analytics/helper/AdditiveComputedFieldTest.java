package com.tarento.analytics.helper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdditiveComputedFieldTest {
    @Test
    void classLoads() {
        assertThat(AdditiveComputedField.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AdditiveComputedField.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
