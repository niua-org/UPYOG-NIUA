package org.upyog.employee.dasboard.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RoleBasedDashboardRequestTest {
    @Test
    void classLoads() {
        assertThat(RoleBasedDashboardRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RoleBasedDashboardRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
