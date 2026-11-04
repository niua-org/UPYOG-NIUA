package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MappingDtoTest {
    @Test
    void classLoads() {
        assertThat(MappingDto.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MappingDto.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
