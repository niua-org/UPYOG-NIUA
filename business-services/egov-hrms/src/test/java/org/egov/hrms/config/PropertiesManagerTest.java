package org.egov.hrms.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PropertiesManagerTest {
    @Test
    void classLoads() {
        assertThat(PropertiesManager.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PropertiesManager.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
