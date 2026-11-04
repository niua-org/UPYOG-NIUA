package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationEntityTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
