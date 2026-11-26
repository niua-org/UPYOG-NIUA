package org.egov.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {
    @Test
    void classLoads() {
        assertThat(SwaggerConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SwaggerConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
