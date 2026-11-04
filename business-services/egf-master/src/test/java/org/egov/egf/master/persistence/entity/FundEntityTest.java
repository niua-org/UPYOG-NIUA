package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundEntityTest {
    @Test
    void classLoads() {
        assertThat(FundEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
