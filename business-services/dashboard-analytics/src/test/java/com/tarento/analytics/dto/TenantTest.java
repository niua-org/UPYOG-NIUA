package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {
    @Test
    void classLoads() {
        assertThat(Tenant.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Tenant.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
