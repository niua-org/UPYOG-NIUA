package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaxHeadMasterTest {
    @Test
    void classLoads() {
        assertThat(TaxHeadMaster.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaxHeadMaster.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
