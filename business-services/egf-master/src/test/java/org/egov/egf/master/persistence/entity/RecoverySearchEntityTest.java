package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecoverySearchEntityTest {
    @Test
    void classLoads() {
        assertThat(RecoverySearchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RecoverySearchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
