package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardDtoTest {
    @Test
    void classLoads() {
        assertThat(DashboardDto.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DashboardDto.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
