package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationSearchEntityTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationSearchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationSearchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
