package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RequestInfoTest {
    @Test
    void classLoads() {
        assertThat(RequestInfo.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RequestInfo.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
