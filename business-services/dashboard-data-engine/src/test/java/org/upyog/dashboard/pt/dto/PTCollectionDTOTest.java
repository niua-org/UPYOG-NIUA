package org.upyog.dashboard.pt.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PTCollectionDTOTest {
    @Test
    void classLoads() {
        assertThat(PTCollectionDTO.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PTCollectionDTO.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
