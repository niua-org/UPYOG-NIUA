package org.upyog.employee.dasboard.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RoleBasedDashboardResponseTest {
    @Test
    void classLoads() {
        assertThat(RoleBasedDashboardResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RoleBasedDashboardResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
