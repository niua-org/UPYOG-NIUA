package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubSchemeRequestTest {
    @Test
    void classLoads() {
        assertThat(SubSchemeRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SubSchemeRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
