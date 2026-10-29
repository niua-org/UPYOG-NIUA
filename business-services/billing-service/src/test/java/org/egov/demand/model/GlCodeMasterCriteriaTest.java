package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GlCodeMasterCriteriaTest {
    @Test
    void classLoads() {
        assertThat(GlCodeMasterCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = GlCodeMasterCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
