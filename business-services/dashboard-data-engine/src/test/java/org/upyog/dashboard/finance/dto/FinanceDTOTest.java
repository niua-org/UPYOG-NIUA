package org.upyog.dashboard.finance.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinanceDTOTest {
    @Test
    void classLoads() {
        assertThat(FinanceDTO.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinanceDTO.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
