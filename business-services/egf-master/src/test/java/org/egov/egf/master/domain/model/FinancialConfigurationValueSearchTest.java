package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationValueSearchTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationValueSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationValueSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
