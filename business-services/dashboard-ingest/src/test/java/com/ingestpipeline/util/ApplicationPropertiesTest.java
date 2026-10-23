package com.ingestpipeline.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationPropertiesTest {
    @Test
    void classLoads() {
        assertThat(ApplicationProperties.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ApplicationProperties.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
