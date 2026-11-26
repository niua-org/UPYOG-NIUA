package org.egov.hrms.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IdResponseTest {
    @Test
    void classLoads() {
        assertThat(IdResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IdResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
