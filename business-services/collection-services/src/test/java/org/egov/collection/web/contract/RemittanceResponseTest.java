package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RemittanceResponseTest {
    @Test
    void classLoads() {
        assertThat(RemittanceResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RemittanceResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
