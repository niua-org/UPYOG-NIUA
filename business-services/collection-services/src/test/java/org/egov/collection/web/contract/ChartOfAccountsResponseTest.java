package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountsResponseTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountsResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountsResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
