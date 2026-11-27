package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountDetailTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
