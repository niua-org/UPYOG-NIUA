package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundsourceEntityTest {
    @Test
    void classLoads() {
        assertThat(FundsourceEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundsourceEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
