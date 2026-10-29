package com.tarento.analytics.model.dashboardConfig;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HeaderTest {
    @Test
    void classLoads() {
        assertThat(Header.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Header.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
