package org.upyog.employee.dasboard.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDashboardDetailsTest {
    @Test
    void classLoads() {
        assertThat(EmployeeDashboardDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EmployeeDashboardDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
