package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundTest {
    @Test
    void classLoads() {
        assertThat(Fund.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Fund.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
