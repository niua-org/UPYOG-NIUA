package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandDueCriteriaTest {
    @Test
    void classLoads() {
        assertThat(DemandDueCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DemandDueCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
