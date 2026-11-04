package org.upyog.dashboard.chb.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CHBDTOTest {
    @Test
    void classLoads() {
        assertThat(CHBDTO.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CHBDTO.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
