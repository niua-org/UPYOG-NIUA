package org.egov.hrms.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IdRequestTest {
    @Test
    void classLoads() {
        assertThat(IdRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IdRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
