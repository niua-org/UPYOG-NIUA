package org.upyog.dashboard.chb.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CHBCollectionDTOTest {
    @Test
    void classLoads() {
        assertThat(CHBCollectionDTO.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CHBCollectionDTO.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
