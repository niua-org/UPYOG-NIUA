package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubSchemeResponseTest {
    @Test
    void classLoads() {
        assertThat(SubSchemeResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SubSchemeResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
