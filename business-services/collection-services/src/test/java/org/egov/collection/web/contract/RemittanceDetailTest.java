package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RemittanceDetailTest {
    @Test
    void classLoads() {
        assertThat(RemittanceDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RemittanceDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
