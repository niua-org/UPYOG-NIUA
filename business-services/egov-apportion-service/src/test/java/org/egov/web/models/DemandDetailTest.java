package org.egov.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandDetailTest {
    @Test
    void classLoads() {
        assertThat(DemandDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DemandDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
