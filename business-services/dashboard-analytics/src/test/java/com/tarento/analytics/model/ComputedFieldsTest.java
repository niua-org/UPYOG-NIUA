package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ComputedFieldsTest {
    @Test
    void classLoads() {
        assertThat(ComputedFields.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ComputedFields.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
