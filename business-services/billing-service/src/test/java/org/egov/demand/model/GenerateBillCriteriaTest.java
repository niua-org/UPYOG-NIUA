package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GenerateBillCriteriaTest {
    @Test
    void classLoads() {
        assertThat(GenerateBillCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = GenerateBillCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
