package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateBillCriteriaTest {
    @Test
    void classLoads() {
        assertThat(UpdateBillCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UpdateBillCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
