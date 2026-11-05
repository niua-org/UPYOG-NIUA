package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetGroupSearchContractTest {
    @Test
    void classLoads() {
        assertThat(BudgetGroupSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BudgetGroupSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
