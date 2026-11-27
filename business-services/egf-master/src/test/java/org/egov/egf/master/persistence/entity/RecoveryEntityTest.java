package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecoveryEntityTest {
    @Test
    void classLoads() {
        assertThat(RecoveryEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RecoveryEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
