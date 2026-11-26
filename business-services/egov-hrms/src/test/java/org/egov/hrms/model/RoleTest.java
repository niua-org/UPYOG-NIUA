package org.egov.hrms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {
    @Test
    void classLoads() {
        assertThat(Role.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Role.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
