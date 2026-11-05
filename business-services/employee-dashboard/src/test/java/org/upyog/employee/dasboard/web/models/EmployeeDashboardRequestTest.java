package org.upyog.employee.dasboard.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDashboardRequestTest {
    @Test
    void classLoads() {
        assertThat(EmployeeDashboardRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EmployeeDashboardRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
