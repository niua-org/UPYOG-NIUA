package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundSearchTest {
    @Test
    void classLoads() {
        assertThat(FundSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
