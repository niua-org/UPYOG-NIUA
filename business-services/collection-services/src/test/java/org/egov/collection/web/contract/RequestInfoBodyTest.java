package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RequestInfoBodyTest {
    @Test
    void classLoads() {
        assertThat(RequestInfoBody.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RequestInfoBody.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
