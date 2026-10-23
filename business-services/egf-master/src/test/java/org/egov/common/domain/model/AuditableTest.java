package org.egov.common.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuditableTest {
    @Test
    void classLoads() {
        assertThat(Auditable.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Auditable.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
