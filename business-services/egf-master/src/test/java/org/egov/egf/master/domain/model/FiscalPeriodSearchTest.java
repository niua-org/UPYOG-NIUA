package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FiscalPeriodSearchTest {
    @Test
    void classLoads() {
        assertThat(FiscalPeriodSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FiscalPeriodSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
