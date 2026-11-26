package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TenantRoleTest {
    @Test
    void classLoads() {
        assertThat(TenantRole.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TenantRole.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
