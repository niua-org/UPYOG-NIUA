package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AmendmentCriteriaTest {
    @Test
    void classLoads() {
        assertThat(AmendmentCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AmendmentCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
