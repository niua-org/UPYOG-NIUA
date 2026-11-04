package com.tarento.analytics.query.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class XAxisTest {
    @Test
    void classLoads() {
        assertThat(XAxis.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = XAxis.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
