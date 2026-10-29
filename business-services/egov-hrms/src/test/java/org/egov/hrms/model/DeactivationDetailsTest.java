package org.egov.hrms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DeactivationDetailsTest {
    @Test
    void classLoads() {
        assertThat(DeactivationDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DeactivationDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
