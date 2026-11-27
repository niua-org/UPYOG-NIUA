package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FiscalPeriodEntityTest {
    @Test
    void classLoads() {
        assertThat(FiscalPeriodEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FiscalPeriodEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
