package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CommonModuleResponseTest {
    @Test
    void classLoads() {
        assertThat(CommonModuleResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CommonModuleResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
