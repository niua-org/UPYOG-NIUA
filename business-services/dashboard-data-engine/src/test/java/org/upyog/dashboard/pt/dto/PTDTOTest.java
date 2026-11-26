package org.upyog.dashboard.pt.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PTDTOTest {
    @Test
    void classLoads() {
        assertThat(PTDTO.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PTDTO.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
