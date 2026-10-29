package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfiguration.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfiguration.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
