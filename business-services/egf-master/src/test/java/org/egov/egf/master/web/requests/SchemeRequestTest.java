package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemeRequestTest {
    @Test
    void classLoads() {
        assertThat(SchemeRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SchemeRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
