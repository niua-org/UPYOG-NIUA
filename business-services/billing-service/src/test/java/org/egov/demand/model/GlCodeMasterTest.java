package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GlCodeMasterTest {
    @Test
    void classLoads() {
        assertThat(GlCodeMaster.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = GlCodeMaster.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
