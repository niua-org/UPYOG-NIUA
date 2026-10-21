package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetGroupResponseTest {
    @Test
    void classLoads() {
        assertThat(BudgetGroupResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BudgetGroupResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
