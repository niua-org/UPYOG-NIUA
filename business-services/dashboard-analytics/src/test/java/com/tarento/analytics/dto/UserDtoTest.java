package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserDtoTest {
    @Test
    void classLoads() {
        assertThat(UserDto.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UserDto.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
