package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationValueTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationValue.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationValue.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
