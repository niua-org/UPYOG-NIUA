package org.upyog.employee.dasboard.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ModuleDashboardDataTest {
    @Test
    void classLoads() {
        assertThat(ModuleDashboardData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ModuleDashboardData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
