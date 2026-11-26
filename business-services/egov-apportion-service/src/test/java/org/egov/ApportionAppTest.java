package org.egov;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApportionAppTest {
    @Test
    void classLoads() {
        assertThat(ApportionApp.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ApportionApp.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
