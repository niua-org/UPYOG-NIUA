package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandDetailCriteriaTest {
    @Test
    void classLoads() {
        assertThat(DemandDetailCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DemandDetailCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
