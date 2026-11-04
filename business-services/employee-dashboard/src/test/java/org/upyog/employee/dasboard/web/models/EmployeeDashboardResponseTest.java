package org.upyog.employee.dasboard.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDashboardResponseTest {
    @Test
    void classLoads() {
        assertThat(EmployeeDashboardResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EmployeeDashboardResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
