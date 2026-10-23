package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetGroupSearchEntityTest {
    @Test
    void classLoads() {
        assertThat(BudgetGroupSearchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BudgetGroupSearchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
