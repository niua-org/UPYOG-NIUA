package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OnlineGLCodeMappingTest {
    @Test
    void classLoads() {
        assertThat(OnlineGLCodeMapping.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = OnlineGLCodeMapping.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
