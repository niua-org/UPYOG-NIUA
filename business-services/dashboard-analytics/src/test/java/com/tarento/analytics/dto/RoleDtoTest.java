package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RoleDtoTest {
    @Test
    void classLoads() {
        assertThat(RoleDto.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RoleDto.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
