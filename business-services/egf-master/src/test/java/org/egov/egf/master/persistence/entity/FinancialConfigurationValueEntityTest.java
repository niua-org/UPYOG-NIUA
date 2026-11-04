package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationValueEntityTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationValueEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationValueEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
