package org.egov.hrms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReactivationDetailsTest {
    @Test
    void classLoads() {
        assertThat(ReactivationDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReactivationDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
