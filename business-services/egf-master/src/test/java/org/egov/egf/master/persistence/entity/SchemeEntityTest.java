package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemeEntityTest {
    @Test
    void classLoads() {
        assertThat(SchemeEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SchemeEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
