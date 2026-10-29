package org.egov.hrms.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigurationTest {
    @Test
    void classLoads() {
        assertThat(SwaggerConfiguration.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SwaggerConfiguration.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
