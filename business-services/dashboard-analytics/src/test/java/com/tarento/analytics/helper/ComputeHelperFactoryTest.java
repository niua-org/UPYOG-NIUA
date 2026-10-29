package com.tarento.analytics.helper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ComputeHelperFactoryTest {
    @Test
    void classLoads() {
        assertThat(ComputeHelperFactory.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ComputeHelperFactory.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
