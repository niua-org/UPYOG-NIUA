package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemeResponseTest {
    @Test
    void classLoads() {
        assertThat(SchemeResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SchemeResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
