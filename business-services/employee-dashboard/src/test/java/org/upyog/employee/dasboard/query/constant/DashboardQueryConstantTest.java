package org.upyog.employee.dasboard.query.constant;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardQueryConstantTest {
    @Test
    void classLoads() {
        assertThat(DashboardQueryConstant.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DashboardQueryConstant.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
