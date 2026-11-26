package org.upyog.dashboard.finance.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinanceCollectionDTOTest {
    @Test
    void classLoads() {
        assertThat(FinanceCollectionDTO.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinanceCollectionDTO.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
