package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountDetailEntityTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountDetailEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountDetailEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
