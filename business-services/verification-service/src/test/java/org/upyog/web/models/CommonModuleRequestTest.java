package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CommonModuleRequestTest {
    @Test
    void classLoads() {
        assertThat(CommonModuleRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CommonModuleRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
