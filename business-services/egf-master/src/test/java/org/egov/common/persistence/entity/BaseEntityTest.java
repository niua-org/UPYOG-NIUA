package org.egov.common.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {
    @Test
    void classLoads() {
        assertThat(BaseEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BaseEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
