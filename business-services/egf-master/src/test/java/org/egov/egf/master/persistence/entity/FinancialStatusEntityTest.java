package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatusEntityTest {
    @Test
    void classLoads() {
        assertThat(FinancialStatusEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialStatusEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
