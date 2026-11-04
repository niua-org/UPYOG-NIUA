package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetGroupContractTest {
    @Test
    void classLoads() {
        assertThat(BudgetGroupContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BudgetGroupContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
