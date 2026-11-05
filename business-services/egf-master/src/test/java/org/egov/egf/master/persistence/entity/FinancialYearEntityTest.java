package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialYearEntityTest {
    @Test
    void classLoads() {
        assertThat(FinancialYearEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialYearEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
