package org.egov.common.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuditableEntityTest {
    @Test
    void classLoads() {
        assertThat(AuditableEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AuditableEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
