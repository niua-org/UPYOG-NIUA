package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StatusResponseTest {
    @Test
    void classLoads() {
        assertThat(StatusResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = StatusResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
