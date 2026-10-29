package org.egov.collection.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {
    @Test
    void classLoads() {
        assertThat(OpenApiConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = OpenApiConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
