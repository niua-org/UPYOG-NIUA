package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountSearchTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
