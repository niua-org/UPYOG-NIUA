package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandCriteriaTest {
    @Test
    void classLoads() {
        assertThat(DemandCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DemandCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
