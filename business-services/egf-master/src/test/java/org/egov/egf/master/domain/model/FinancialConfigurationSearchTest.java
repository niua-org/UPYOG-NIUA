package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationSearchTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
