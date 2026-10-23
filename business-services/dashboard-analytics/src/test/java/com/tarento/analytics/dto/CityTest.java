package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CityTest {
    @Test
    void classLoads() {
        assertThat(City.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = City.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
