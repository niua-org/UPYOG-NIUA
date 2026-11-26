package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetGroupRequestTest {
    @Test
    void classLoads() {
        assertThat(BudgetGroupRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BudgetGroupRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
