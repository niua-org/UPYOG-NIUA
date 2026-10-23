package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountDetailSearchTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountDetailSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountDetailSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
