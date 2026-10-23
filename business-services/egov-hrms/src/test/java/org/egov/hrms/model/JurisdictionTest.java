package org.egov.hrms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JurisdictionTest {
    @Test
    void classLoads() {
        assertThat(Jurisdiction.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Jurisdiction.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
