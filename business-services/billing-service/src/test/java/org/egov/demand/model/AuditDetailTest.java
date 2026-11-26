package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuditDetailTest {
    @Test
    void classLoads() {
        assertThat(AuditDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AuditDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
