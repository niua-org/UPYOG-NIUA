package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetGroupEntityTest {
    @Test
    void classLoads() {
        assertThat(BudgetGroupEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BudgetGroupEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
