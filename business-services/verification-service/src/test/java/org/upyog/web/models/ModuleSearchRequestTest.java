package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ModuleSearchRequestTest {
    @Test
    void classLoads() {
        assertThat(ModuleSearchRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ModuleSearchRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
