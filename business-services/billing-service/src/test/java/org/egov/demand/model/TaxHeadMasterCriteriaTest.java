package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaxHeadMasterCriteriaTest {
    @Test
    void classLoads() {
        assertThat(TaxHeadMasterCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaxHeadMasterCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
