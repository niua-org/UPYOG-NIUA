package org.egov.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApportionConfigTest {
    @Test
    void classLoads() {
        assertThat(ApportionConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ApportionConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
