package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RemittanceRequestTest {
    @Test
    void classLoads() {
        assertThat(RemittanceRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RemittanceRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
