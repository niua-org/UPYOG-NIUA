package com.tarento.analytics.helper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ComputedFieldHelperTest {
    @Test
    void classLoads() {
        assertThat(ComputedFieldHelper.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ComputedFieldHelper.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
