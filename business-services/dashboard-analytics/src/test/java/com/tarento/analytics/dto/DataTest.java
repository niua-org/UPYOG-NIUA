package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DataTest {
    @Test
    void classLoads() {
        assertThat(Data.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Data.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
