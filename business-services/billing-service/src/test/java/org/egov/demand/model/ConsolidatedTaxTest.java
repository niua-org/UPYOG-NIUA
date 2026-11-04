package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConsolidatedTaxTest {
    @Test
    void classLoads() {
        assertThat(ConsolidatedTax.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ConsolidatedTax.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
