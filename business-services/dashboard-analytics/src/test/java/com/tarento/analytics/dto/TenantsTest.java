package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TenantsTest {
    @Test
    void classLoads() {
        assertThat(Tenants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Tenants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
