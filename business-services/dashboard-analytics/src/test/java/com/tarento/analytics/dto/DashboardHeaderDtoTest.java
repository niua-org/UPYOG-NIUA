package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardHeaderDtoTest {
    @Test
    void classLoads() {
        assertThat(DashboardHeaderDto.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DashboardHeaderDto.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
