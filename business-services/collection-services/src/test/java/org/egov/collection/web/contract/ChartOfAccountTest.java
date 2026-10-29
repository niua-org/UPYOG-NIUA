package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccount.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccount.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
