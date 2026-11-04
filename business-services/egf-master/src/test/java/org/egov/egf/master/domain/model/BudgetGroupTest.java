package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetGroupTest {
    @Test
    void classLoads() {
        assertThat(BudgetGroup.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BudgetGroup.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
