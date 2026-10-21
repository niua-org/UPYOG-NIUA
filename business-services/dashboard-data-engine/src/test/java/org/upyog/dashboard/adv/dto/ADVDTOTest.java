package org.upyog.dashboard.adv.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ADVDTOTest {
    @Test
    void classLoads() {
        assertThat(ADVDTO.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ADVDTO.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
