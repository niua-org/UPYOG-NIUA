package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionarySearchEntityTest {
    @Test
    void classLoads() {
        assertThat(FunctionarySearchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionarySearchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
