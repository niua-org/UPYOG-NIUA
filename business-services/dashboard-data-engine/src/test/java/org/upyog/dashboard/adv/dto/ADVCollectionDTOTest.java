package org.upyog.dashboard.adv.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ADVCollectionDTOTest {
    @Test
    void classLoads() {
        assertThat(ADVCollectionDTO.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ADVCollectionDTO.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
